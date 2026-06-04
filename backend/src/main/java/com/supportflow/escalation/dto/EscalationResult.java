package com.supportflow.escalation.dto;

import com.supportflow.ticket.dto.TicketResponse;
import java.util.List;

/**
 * Result of a {@code POST /api/tickets/{id}/escalate}. Always returned with HTTP 200; the caller
 * reads {@code status} to render badges and decide whether to offer Retry (mirrors the AI advisor's
 * envelope). {@code status} is ESCALATED (all three tools linked), PARTIAL (some linked, some
 * failed), or FAILED (none linked).
 */
public record EscalationResult(String status, List<ToolOutcome> targets, TicketResponse ticket) {

    public static final String ESCALATED = "ESCALATED";
    public static final String PARTIAL = "PARTIAL";
    public static final String FAILED = "FAILED";
}
