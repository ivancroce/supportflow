package com.supportflow.ai;

import com.supportflow.ticket.Ticket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "ai_suggestions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSuggestion {

    @Id
    @Column(name = "ticket_id")
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

    // Refreshed on every (re)generation so the UI can surface staleness — see ADR 0002.
    @UpdateTimestamp
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public AiSuggestion(Ticket ticket, String suggestedType, String suggestedCategory,
                        String suggestedPriority, String draftNote) {
        this.ticket = Objects.requireNonNull(ticket, "ticket");
        this.suggestedType = suggestedType;
        this.suggestedCategory = suggestedCategory;
        this.suggestedPriority = suggestedPriority;
        this.draftNote = draftNote;
    }

    /** Overwrite the cached suggestion with a freshly generated one. */
    public void replaceSuggestion(String suggestedType, String suggestedCategory,
                                  String suggestedPriority, String draftNote) {
        this.suggestedType = suggestedType;
        this.suggestedCategory = suggestedCategory;
        this.suggestedPriority = suggestedPriority;
        this.draftNote = draftNote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AiSuggestion that)) return false;
        return getTicketId() != null && getTicketId().equals(that.getTicketId());
    }

    @Override
    public int hashCode() {
        return AiSuggestion.class.hashCode();
    }
}
