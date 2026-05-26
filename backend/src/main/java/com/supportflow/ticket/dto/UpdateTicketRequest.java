package com.supportflow.ticket.dto;

import com.supportflow.ticket.TicketPriority;
import com.supportflow.ticket.TicketStatus;
import com.supportflow.ticket.TicketType;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(min = 1, max = 500) String subject,
        @Size(max = 10_000) String description,
        TicketStatus status,
        TicketPriority priority,
        TicketType type,
        @Size(max = 128) String category) {}
