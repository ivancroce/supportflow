package com.supportflow.ticket;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
