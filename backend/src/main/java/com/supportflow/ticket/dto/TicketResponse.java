package com.supportflow.ticket.dto;

import com.supportflow.ticket.Ticket;
import com.supportflow.ticket.TicketPriority;
import com.supportflow.ticket.TicketStatus;
import com.supportflow.ticket.TicketType;
import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID projectId,
        String subject,
        String description,
        TicketStatus status,
        TicketPriority priority,
        TicketType type,
        String category,
        boolean escalated,
        String jiraIssueKey,
        String qaseCaseId,
        String confluencePageId,
        Instant createdAt,
        Instant updatedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getProject().getId(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getType(),
                ticket.getCategory(),
                ticket.isEscalated(),
                ticket.getJiraIssueKey(),
                ticket.getQaseCaseId(),
                ticket.getConfluencePageId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
