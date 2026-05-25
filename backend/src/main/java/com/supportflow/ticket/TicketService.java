package com.supportflow.ticket;

import com.supportflow.project.Project;
import com.supportflow.project.ProjectService;
import com.supportflow.ticket.dto.CreateTicketRequest;
import com.supportflow.ticket.dto.TicketResponse;
import java.util.UUID;
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
}
