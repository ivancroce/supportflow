package com.supportflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "ai_suggestions")
@Getter
@Setter
@NoArgsConstructor
public class AiSuggestion {

    @Id
    @Column(name = "ticket_id")
    @Setter(AccessLevel.NONE)
    private UUID ticketId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "suggested_type", length = 32)
    private String suggestedType;

    @Column(name = "suggested_category", length = 128)
    private String suggestedCategory;

    @Column(name = "suggested_priority", length = 32)
    private String suggestedPriority;

    @Column(name = "draft_note", columnDefinition = "text")
    private String draftNote;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    public AiSuggestion(Ticket ticket, String suggestedType, String suggestedCategory,
                        String suggestedPriority, String draftNote) {
        this.ticket = ticket;
        this.suggestedType = suggestedType;
        this.suggestedCategory = suggestedCategory;
        this.suggestedPriority = suggestedPriority;
        this.draftNote = draftNote;
    }
}
