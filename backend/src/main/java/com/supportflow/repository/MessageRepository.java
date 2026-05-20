package com.supportflow.repository;

import com.supportflow.entity.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
