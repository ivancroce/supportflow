package com.supportflow.ticket;

import com.supportflow.ticket.dto.CreateTicketRequest;
import com.supportflow.ticket.dto.TicketResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/projects/{projectId}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(userId, projectId, request);
    }
}
