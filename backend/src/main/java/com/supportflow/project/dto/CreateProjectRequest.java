package com.supportflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String clientName,
        @Size(max = 10_000) String description,
        @Size(max = 64) String jiraProjectKey,
        @Size(max = 64) String qaseProjectCode,
        @Size(max = 64) String confluenceSpaceKey) {
}
