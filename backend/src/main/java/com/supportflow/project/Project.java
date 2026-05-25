package com.supportflow.project;

import com.supportflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "client_name", length = 255)
    private String clientName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "jira_project_key", length = 64)
    private String jiraProjectKey;

    @Column(name = "qase_project_code", length = 64)
    private String qaseProjectCode;

    @Column(name = "confluence_space_key", length = 64)
    private String confluenceSpaceKey;

    @Column(nullable = false)
    private boolean archived;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Project(User owner, String name, String clientName, String description) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.name = requireNonBlank(name, "name");
        this.clientName = clientName;
        this.description = description;
    }

    public void rename(String name) {
        this.name = requireNonBlank(name, "name");
    }

    public void updateClientName(String clientName) {
        this.clientName = clientName;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateJiraProjectKey(String jiraProjectKey) {
        this.jiraProjectKey = jiraProjectKey;
    }

    public void updateQaseProjectCode(String qaseProjectCode) {
        this.qaseProjectCode = qaseProjectCode;
    }

    public void updateConfluenceSpaceKey(String confluenceSpaceKey) {
        this.confluenceSpaceKey = confluenceSpaceKey;
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
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
        if (!(o instanceof Project that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Project.class.hashCode();
    }
}
