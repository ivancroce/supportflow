package com.supportflow.escalation;

import com.supportflow.ai.AiAdvisorService;
import com.supportflow.escalation.client.ConfluenceClient;
import com.supportflow.escalation.client.ConfluenceClient.ConfluencePage;
import com.supportflow.escalation.client.ConfluenceClient.ConfluencePageRequest;
import com.supportflow.escalation.client.IntegrationException;
import com.supportflow.escalation.client.JiraClient;
import com.supportflow.escalation.client.JiraClient.JiraIssue;
import com.supportflow.escalation.client.JiraClient.JiraIssueRequest;
import com.supportflow.escalation.client.QaseClient;
import com.supportflow.escalation.client.QaseClient.QaseCase;
import com.supportflow.escalation.client.QaseClient.QaseCaseRequest;
import com.supportflow.escalation.dto.EscalationResult;
import com.supportflow.escalation.dto.ToolOutcome;
import com.supportflow.project.Project;
import com.supportflow.ticket.Ticket;
import com.supportflow.ticket.TicketService;
import com.supportflow.ticket.dto.TicketResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pushes an escalated ticket into Jira, Qase, and Confluence (one-way, Phase 1). The headline
 * feature. Design recorded in ADR 0003:
 *
 * <ul>
 *   <li><b>Fail fast on config</b> — all three per-project targets must be set or we reject before
 *       any external call (and before any ticket mutation).</li>
 *   <li><b>External calls run outside any DB transaction</b> (as in {@code AiAdvisorService}) so a
 *       slow upstream never pins a JDBC connection; each created id is persisted via a short
 *       {@code TicketService} call as it succeeds.</li>
 *   <li><b>Idempotent retry</b> — a tool already linked (id present) is skipped, so re-running
 *       escalate never double-creates. {@code escalated} flips true only once all three exist.</li>
 *   <li><b>Always-200 envelope</b> — partial failures are reported per tool, not thrown.</li>
 * </ul>
 *
 * <p>Ticket persistence goes through {@link TicketService} and the cached Triage Note is read via
 * {@link AiAdvisorService} so each feature's repository stays package-private.
 */
@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    static final String JIRA = "JIRA";
    static final String QASE = "QASE";
    static final String CONFLUENCE = "CONFLUENCE";

    private final TicketService ticketService;
    private final AiAdvisorService aiAdvisorService;
    private final JiraClient jiraClient;
    private final QaseClient qaseClient;
    private final ConfluenceClient confluenceClient;
    private final TransactionTemplate txTemplate;
    private final String appBaseUrl;
    private final String jiraIssueType;

    public EscalationService(
            TicketService ticketService,
            AiAdvisorService aiAdvisorService,
            JiraClient jiraClient,
            QaseClient qaseClient,
            ConfluenceClient confluenceClient,
            PlatformTransactionManager txManager,
            @Value("${supportflow.app.base-url:http://localhost:5173}") String appBaseUrl,
            @Value("${supportflow.integrations.jira.issue-type:Bug}") String jiraIssueType) {
        this.ticketService = ticketService;
        this.aiAdvisorService = aiAdvisorService;
        this.jiraClient = jiraClient;
        this.qaseClient = qaseClient;
        this.confluenceClient = confluenceClient;
        this.txTemplate = new TransactionTemplate(txManager);
        this.appBaseUrl = stripTrailingSlash(appBaseUrl);
        this.jiraIssueType = jiraIssueType;
    }

    public EscalationResult escalate(UUID ownerId, UUID ticketId) {
        // 1. Validate ownership + config and snapshot everything the external calls need, in one short
        //    read tx so the connection is released before the HTTP round-trips. Throws (422) before
        //    any mutation if a target is unconfigured.
        Snapshot snap = txTemplate.execute(status -> {
            Ticket ticket = ticketService.getOwnedTicket(ownerId, ticketId);
            Project project = ticket.getProject();
            requireConfigured(project);
            String triageNote = aiAdvisorService.cachedTriageNote(ticketId).orElse(null);
            return new Snapshot(
                    ticket.getSubject(),
                    ticket.getDescription(),
                    ticket.getPriority().name(),
                    project.getJiraProjectKey(),
                    project.getQaseProjectCode(),
                    project.getConfluenceSpaceKey(),
                    ticket.getJiraIssueKey(),
                    ticket.getQaseCaseId(),
                    ticket.getConfluencePageId(),
                    triageNote);
        });

        // 2. Clicking "Escalate as bug" is the declaration — mark it up front (see ADR 0003).
        ticketService.markAsBug(ownerId, ticketId);

        String backlink = appBaseUrl + "/tickets/" + ticketId;
        List<ToolOutcome> outcomes = new ArrayList<>();

        // 3. Jira first — it's the anchor the other two reference. Capture the key (existing or new).
        String jiraKey = snap.jiraIssueKey();
        if (jiraKey != null) {
            // Already linked by a prior escalation; the stored key identifies the issue. We don't
            // reconstruct the browse URL here (Qase/Confluence URLs aren't reconstructable either,
            // so all three already-linked outcomes are URL-less for consistency).
            outcomes.add(ToolOutcome.alreadyLinked(JIRA, jiraKey, null));
        } else {
            try {
                JiraIssue issue = jiraClient.createIssue(new JiraIssueRequest(
                        snap.jiraProjectKey(), jiraIssueType, snap.subject(),
                        body(snap, backlink, null)));
                ticketService.recordJiraIssue(ownerId, ticketId, issue.key());
                jiraKey = issue.key();
                outcomes.add(ToolOutcome.created(JIRA, issue.key(), issue.url()));
            } catch (IntegrationException e) {
                log.warn("Jira escalation failed for ticket {}: {}", ticketId, e.getMessage());
                outcomes.add(ToolOutcome.failed(JIRA, e.getMessage()));
            }
        }

        // 4. Qase (references the Jira key when available).
        if (snap.qaseCaseId() != null) {
            outcomes.add(ToolOutcome.alreadyLinked(QASE, snap.qaseCaseId(), null));
        } else {
            try {
                QaseCase qaseCase = qaseClient.createCase(new QaseCaseRequest(
                        snap.qaseProjectCode(), snap.subject(), body(snap, backlink, jiraKey)));
                ticketService.recordQaseCase(ownerId, ticketId, qaseCase.id());
                outcomes.add(ToolOutcome.created(QASE, qaseCase.id(), qaseCase.url()));
            } catch (IntegrationException e) {
                log.warn("Qase escalation failed for ticket {}: {}", ticketId, e.getMessage());
                outcomes.add(ToolOutcome.failed(QASE, e.getMessage()));
            }
        }

        // 5. Confluence Known Issue page (references the Jira key when available).
        if (snap.confluencePageId() != null) {
            outcomes.add(ToolOutcome.alreadyLinked(CONFLUENCE, snap.confluencePageId(), null));
        } else {
            try {
                ConfluencePage page = confluenceClient.createKnownIssue(new ConfluencePageRequest(
                        snap.confluenceSpaceKey(), "Known Issue: " + snap.subject(),
                        body(snap, backlink, jiraKey)));
                ticketService.recordConfluencePage(ownerId, ticketId, page.id());
                outcomes.add(ToolOutcome.created(CONFLUENCE, page.id(), page.url()));
            } catch (IntegrationException e) {
                log.warn("Confluence escalation failed for ticket {}: {}", ticketId, e.getMessage());
                outcomes.add(ToolOutcome.failed(CONFLUENCE, e.getMessage()));
            }
        }

        // 6. Flip escalated if all three are now linked, and return the fresh ticket state.
        TicketResponse ticket = ticketService.markEscalatedIfComplete(ownerId, ticketId);
        return new EscalationResult(overallStatus(outcomes), outcomes, ticket);
    }

    private void requireConfigured(Project project) {
        List<String> missing = new ArrayList<>();
        if (isBlank(project.getJiraProjectKey())) {
            missing.add("jiraProjectKey");
        }
        if (isBlank(project.getQaseProjectCode())) {
            missing.add("qaseProjectCode");
        }
        if (isBlank(project.getConfluenceSpaceKey())) {
            missing.add("confluenceSpaceKey");
        }
        if (!missing.isEmpty()) {
            throw new EscalationNotConfiguredException(missing);
        }
    }

    /**
     * Assemble the body shared by the Jira issue, Qase case, and Confluence page: the back-link, the
     * SupportFlow priority (not mapped onto Jira's priority field — see ADR 0003), the ticket
     * description, and the cached Triage Note when one exists. {@code jiraKey} is included for Qase
     * and Confluence (which reference the anchor issue) and null for the Jira issue itself.
     */
    private static String body(Snapshot snap, String backlink, String jiraKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("SupportFlow ticket: ").append(backlink).append("\n");
        if (jiraKey != null) {
            sb.append("Jira issue: ").append(jiraKey).append("\n");
        }
        sb.append("Priority: ").append(snap.priorityName()).append("\n");
        if (!isBlank(snap.description())) {
            sb.append("\n").append(snap.description()).append("\n");
        }
        if (!isBlank(snap.triageNote())) {
            sb.append("\nTriage summary:\n").append(snap.triageNote()).append("\n");
        }
        return sb.toString();
    }

    private static String overallStatus(List<ToolOutcome> outcomes) {
        long linked = outcomes.stream().filter(ToolOutcome::linked).count();
        if (linked == outcomes.size()) {
            return EscalationResult.ESCALATED;
        }
        return linked == 0 ? EscalationResult.FAILED : EscalationResult.PARTIAL;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Immutable snapshot of the ticket/project fields needed for the (out-of-transaction) calls. */
    private record Snapshot(
            String subject,
            String description,
            String priorityName,
            String jiraProjectKey,
            String qaseProjectCode,
            String confluenceSpaceKey,
            String jiraIssueKey,
            String qaseCaseId,
            String confluencePageId,
            String triageNote) {
    }
}
