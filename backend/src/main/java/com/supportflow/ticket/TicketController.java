package com.supportflow.ticket;

import com.supportflow.ticket.dto.CreateTicketRequest;
import com.supportflow.ticket.dto.TicketResponse;
import com.supportflow.ticket.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/projects/{projectId}/tickets")
    public Page<TicketResponse> list(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ticketService.list(userId, projectId, pageable);
    }

    @PostMapping("/projects/{projectId}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(userId, projectId, request);
    }

    @GetMapping("/tickets/{id}")
    public TicketResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return ticketService.get(userId, id);
    }

    @PatchMapping("/tickets/{id}")
    public TicketResponse update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.update(userId, id, request);
    }

    @DeleteMapping("/tickets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        ticketService.delete(userId, id);
    }
}
