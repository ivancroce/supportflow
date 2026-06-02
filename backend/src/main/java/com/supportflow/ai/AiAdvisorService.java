package com.supportflow.ai;

import com.supportflow.ai.dto.AiSuggestionEnvelope;
import com.supportflow.ai.dto.AiSuggestionResponse;
import com.supportflow.project.Project;
import com.supportflow.ticket.Ticket;
import com.supportflow.ticket.TicketService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(AiAdvisorService.class);

    private final TicketService ticketService;
    private final AiSuggestionRepository suggestionRepository;
    private final GeminiClient geminiClient;
    private final TransactionTemplate txTemplate;

    public AiAdvisorService(TicketService ticketService,
                            AiSuggestionRepository suggestionRepository,
                            GeminiClient geminiClient,
                            PlatformTransactionManager txManager) {
        this.ticketService = ticketService;
        this.suggestionRepository = suggestionRepository;
        this.geminiClient = geminiClient;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    public AiSuggestionEnvelope getSuggestion(UUID ownerId, UUID ticketId) {
        GenerationInput prep = txTemplate.execute(status -> {
            Ticket ticket = ticketService.getOwnedTicket(ownerId, ticketId);
            AiSuggestion cached = suggestionRepository.findById(ticket.getId()).orElse(null);
            if (cached != null) {
                return new GenerationInput(null, AiSuggestionResponse.from(cached));
            }
            return new GenerationInput(toInput(ticket), null);
        });
        if (prep.cached() != null) {
            return AiSuggestionEnvelope.ready(prep.cached());
        }
        return callGeminiAndStore(ownerId, ticketId, prep.input());
    }

    public AiSuggestionEnvelope regenerate(UUID ownerId, UUID ticketId) {
        // Verify ownership and capture the prompt input inside a short read tx; release the
        // connection before the multi-second Gemini call.
        GeminiInput input = txTemplate.execute(status ->
                toInput(ticketService.getOwnedTicket(ownerId, ticketId)));
        return callGeminiAndStore(ownerId, ticketId, input);
    }

    /**
     * Runs the Gemini HTTP call OUTSIDE any DB transaction so we don't pin a connection
     * to a slow network round-trip. On success a second short tx upserts the cache row.
     */
    private AiSuggestionEnvelope callGeminiAndStore(UUID ownerId, UUID ticketId, GeminiInput input) {
        GeminiSuggestion generated;
        try {
            generated = geminiClient.classify(input);
        } catch (GeminiQuotaException e) {
            log.warn("Gemini quota exhausted while classifying ticket {}: {}", ticketId, e.getMessage());
            return AiSuggestionEnvelope.failed("QUOTA");
        } catch (GeminiException e) {
            // Transient upstream failures (notably free-tier 503 "model overloaded") are common here;
            // log the message only, not the full stack — the ~130-frame filter-chain trace is noise.
            log.warn("Gemini classify failed for ticket {}: {}", ticketId, e.getMessage());
            return AiSuggestionEnvelope.failed("GENERATION_FAILED");
        }

        AiSuggestionResponse saved;
        try {
            saved = txTemplate.execute(status -> upsertSuggestion(ownerId, ticketId, generated));
        } catch (DataIntegrityViolationException race) {
            // A concurrent (re)generate inserted the cache row between our findById and flush
            // (ticket_id is the PK). The row now exists, so a second attempt takes the update path.
            log.debug("Concurrent AI suggestion insert for ticket {}; retrying as update", ticketId);
            saved = txTemplate.execute(status -> upsertSuggestion(ownerId, ticketId, generated));
        }
        return AiSuggestionEnvelope.ready(saved);
    }

    /** Insert the suggestion row, or overwrite it if one already exists. Must run inside a tx. */
    private AiSuggestionResponse upsertSuggestion(UUID ownerId, UUID ticketId, GeminiSuggestion generated) {
        Ticket ticket = ticketService.getOwnedTicket(ownerId, ticketId);
        AiSuggestion existing = suggestionRepository.findById(ticket.getId()).orElse(null);
        AiSuggestion row;
        if (existing != null) {
            existing.replaceSuggestion(
                    generated.suggestedType(),
                    generated.suggestedCategory(),
                    generated.suggestedPriority(),
                    generated.draftNote());
            row = suggestionRepository.saveAndFlush(existing);
        } else {
            row = suggestionRepository.saveAndFlush(new AiSuggestion(
                    ticket,
                    generated.suggestedType(),
                    generated.suggestedCategory(),
                    generated.suggestedPriority(),
                    generated.draftNote()));
        }
        return AiSuggestionResponse.from(row);
    }

    private static GeminiInput toInput(Ticket ticket) {
        Project project = ticket.getProject();
        return new GeminiInput(
                ticket.getSubject(),
                ticket.getDescription(),
                project.getName(),
                project.getClientName(),
                project.getDescription());
    }

    /** Carries either the input needed for a Gemini call, or a cached response — never both. */
    private record GenerationInput(GeminiInput input, AiSuggestionResponse cached) {
    }
}
