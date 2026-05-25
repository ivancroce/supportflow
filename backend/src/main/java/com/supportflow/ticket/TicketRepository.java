package com.supportflow.ticket;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByProjectId(UUID projectId);
}
