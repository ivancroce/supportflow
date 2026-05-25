package com.supportflow.ticket.dto;

import com.supportflow.ticket.TicketPriority;
import com.supportflow.ticket.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 500) String subject,
        String description,
        TicketPriority priority,
        TicketType type,
        @Size(max = 128) String category) {}
