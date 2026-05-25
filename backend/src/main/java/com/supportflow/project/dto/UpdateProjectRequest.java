package com.supportflow.project.dto;

import jakarta.validation.constraints.Size;

/**
 * PATCH payload — every field is optional. A {@code null} field means "leave unchanged"; only
 * non-null fields are applied to the project.
 */
public record UpdateProjectRequest(
        @Size(max = 255) String name,
        @Size(max = 255) String clientName,
        String description,
        @Size(max = 64) String jiraProjectKey,
        @Size(max = 64) String qaseProjectCode,
        @Size(max = 64) String confluenceSpaceKey,
        Boolean archived) {
}
