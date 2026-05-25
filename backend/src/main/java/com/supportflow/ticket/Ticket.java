package com.supportflow.ticket;

import com.supportflow.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketType type;

    @Column(length = 128)
    private String category;

    @Column(nullable = false)
    private boolean escalated;

    @Column(name = "jira_issue_key", length = 64)
    private String jiraIssueKey;

    @Column(name = "qase_case_id", length = 64)
    private String qaseCaseId;

    @Column(name = "confluence_page_id", length = 64)
    private String confluencePageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Ticket(Project project, String subject, String description,
                  TicketPriority priority, TicketType type, String category) {
        this.project = Objects.requireNonNull(project, "project");
        this.subject = requireNonBlank(subject, "subject");
        this.description = description;
        this.status = TicketStatus.OPEN;
        this.priority = priority != null ? priority : TicketPriority.MEDIUM;
        this.type = type != null ? type : TicketType.QUESTION;
        this.category = category;
    }

    public void changeStatus(TicketStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void changePriority(TicketPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority");
    }

    public void categorize(String category) {
        this.category = category;
    }

    /** Mark this ticket as escalated and record the external system identifiers it was linked to. */
    public void markEscalated(String jiraIssueKey, String qaseCaseId, String confluencePageId) {
        this.escalated = true;
        this.jiraIssueKey = jiraIssueKey;
        this.qaseCaseId = qaseCaseId;
        this.confluencePageId = confluencePageId;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Ticket.class.hashCode();
    }
}
