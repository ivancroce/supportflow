package com.supportflow.repository;

import com.supportflow.entity.Ticket;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByProjectId(UUID projectId);
}
