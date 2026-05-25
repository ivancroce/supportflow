package com.supportflow.project.dto;

import com.supportflow.project.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String clientName,
        String description,
        String jiraProjectKey,
        String qaseProjectCode,
        String confluenceSpaceKey,
        boolean archived,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getClientName(),
                project.getDescription(),
                project.getJiraProjectKey(),
                project.getQaseProjectCode(),
                project.getConfluenceSpaceKey(),
                project.isArchived(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
