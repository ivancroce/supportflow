package com.supportflow.ticket;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByProjectId(UUID projectId, Pageable pageable);
}
