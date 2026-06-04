package com.supportflow.ticket;

import com.supportflow.exception.NotFoundException;
import com.supportflow.project.Project;
import com.supportflow.project.ProjectService;
import com.supportflow.ticket.dto.CreateTicketRequest;
import com.supportflow.ticket.dto.TicketResponse;
import com.supportflow.ticket.dto.UpdateTicketRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectService projectService;

    public TicketService(TicketRepository ticketRepository, ProjectService projectService) {
        this.ticketRepository = ticketRepository;
        this.projectService = projectService;
    }

    @Transactional
    public TicketResponse create(UUID ownerId, UUID projectId, CreateTicketRequest request) {
        Project project = projectService.getOwnedProject(ownerId, projectId);
        Ticket ticket = new Ticket(
                project,
                request.subject(),
                request.description(),
                request.priority(),
                request.type(),
                request.category());
        return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(UUID ownerId, UUID projectId, Pageable pageable) {
        // Verify ownership of the project first; if it isn't theirs, surface 404 (don't leak ids).
        projectService.getOwnedProject(ownerId, projectId);
        return ticketRepository.findByProjectId(projectId, pageable).map(TicketResponse::from);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(UUID ownerId, UUID ticketId) {
        return TicketResponse.from(requireOwned(ownerId, ticketId));
    }

    @Transactional
    public TicketResponse update(UUID ownerId, UUID ticketId, UpdateTicketRequest request) {
        Ticket ticket = requireOwned(ownerId, ticketId);
        if (request.subject() != null) {
            ticket.updateSubject(request.subject());
        }
        if (request.description() != null) {
            ticket.updateDescription(request.description());
        }
        if (request.status() != null) {
            ticket.changeStatus(request.status());
        }
        if (request.priority() != null) {
            ticket.changePriority(request.priority());
        }
        if (request.type() != null) {
            ticket.changeType(request.type());
        }
        if (request.category() != null) {
            ticket.categorize(request.category());
        }
        // Flush so @UpdateTimestamp reflects this change in the returned DTO.
        return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }

    @Transactional
    public void delete(UUID ownerId, UUID ticketId) {
        Ticket ticket = requireOwned(ownerId, ticketId);
        ticketRepository.delete(ticket);
    }

    /**
     * Owner-scoped entity lookup for collaborating services (e.g. AI advisor, escalation).
     * Reports not-found rather than forbidden so we don't leak that the id exists.
     */
    @Transactional(readOnly = true)
    public Ticket getOwnedTicket(UUID ownerId, UUID ticketId) {
        return requireOwned(ownerId, ticketId);
    }

    // --- Escalation support: owner-scoped state transitions used by EscalationService. Kept here so
    // the TicketRepository stays encapsulated within this package (see ADR 0003). ---

    /** Mark the ticket a bug (idempotent). Called up front when escalation begins. */
    @Transactional
    public void markAsBug(UUID ownerId, UUID ticketId) {
        Ticket ticket = requireOwned(ownerId, ticketId);
        ticket.markAsBug();
        ticketRepository.saveAndFlush(ticket);
    }

    /** Record the created Jira issue key on the ticket (one short tx, as escalation succeeds). */
    @Transactional
    public void recordJiraIssue(UUID ownerId, UUID ticketId, String jiraIssueKey) {
        Ticket ticket = requireOwned(ownerId, ticketId);
        ticket.recordJiraIssue(jiraIssueKey);
        ticketRepository.saveAndFlush(ticket);
    }

    /** Record the created Qase case id on the ticket. */
    @Transactional
    public void recordQaseCase(UUID ownerId, UUID ticketId, String qaseCaseId) {
        Ticket ticket = requireOwned(ownerId, ticketId);
        ticket.recordQaseCase(qaseCaseId);
        ticketRepository.saveAndFlush(ticket);
    }

    /** Record the created Confluence page id on the ticket. */
    @Transactional
    public void recordConfluencePage(UUID ownerId, UUID ticketId, String confluencePageId) {
        Ticket ticket = requireOwned(ownerId, ticketId);
        ticket.recordConfluencePage(confluencePageId);
        ticketRepository.saveAndFlush(ticket);
    }

    /** Flip the escalated flag iff all three external ids now exist, and return the fresh ticket. */
    @Transactional
    public TicketResponse markEscalatedIfComplete(UUID ownerId, UUID ticketId) {
        Ticket ticket = requireOwned(ownerId, ticketId);
        ticket.markEscalatedIfComplete();
        return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }

    private Ticket requireOwned(UUID ownerId, UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .filter(ticket -> ticket.getProject().getOwner().getId().equals(ownerId))
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
    }
}
