package com.supportflow.entity;

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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
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
        this.owner = owner;
        this.name = name;
        this.clientName = clientName;
        this.description = description;
    }
}
