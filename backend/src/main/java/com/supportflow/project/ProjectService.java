package com.supportflow.project;

import com.supportflow.exception.NotFoundException;
import com.supportflow.project.dto.CreateProjectRequest;
import com.supportflow.project.dto.ProjectResponse;
import com.supportflow.project.dto.UpdateProjectRequest;
import com.supportflow.user.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID ownerId) {
        return projectRepository.findByOwnerId(ownerId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID ownerId, UUID projectId) {
        return ProjectResponse.from(requireOwned(ownerId, projectId));
    }

    /**
     * Cross-feature lookup: return the {@link Project} only if the caller owns it. Use this from
     * other services (e.g., ticket creation) that need to verify ownership before linking to the
     * project. Owner-mismatch is reported as not-found, same as {@link #get}, so project ids
     * don't leak.
     */
    @Transactional(readOnly = true)
    public Project getOwnedProject(UUID ownerId, UUID projectId) {
        return requireOwned(ownerId, projectId);
    }

    @Transactional
    public ProjectResponse create(UUID ownerId, CreateProjectRequest request) {
        Project project = new Project(
                userService.getReference(ownerId),
                request.name(),
                request.clientName(),
                request.description());
        project.updateJiraProjectKey(request.jiraProjectKey());
        project.updateQaseProjectCode(request.qaseProjectCode());
        project.updateConfluenceSpaceKey(request.confluenceSpaceKey());
        // Flush so Hibernate's @CreationTimestamp/@UpdateTimestamp are populated on the returned DTO.
        return ProjectResponse.from(projectRepository.saveAndFlush(project));
    }

    @Transactional
    public ProjectResponse update(UUID ownerId, UUID projectId, UpdateProjectRequest request) {
        Project project = requireOwned(ownerId, projectId);
        if (request.name() != null && !request.name().isBlank()) {
            project.rename(request.name());
        }
        if (request.clientName() != null) {
            project.updateClientName(request.clientName());
        }
        if (request.description() != null) {
            project.updateDescription(request.description());
        }
        if (request.jiraProjectKey() != null) {
            project.updateJiraProjectKey(request.jiraProjectKey());
        }
        if (request.qaseProjectCode() != null) {
            project.updateQaseProjectCode(request.qaseProjectCode());
        }
        if (request.confluenceSpaceKey() != null) {
            project.updateConfluenceSpaceKey(request.confluenceSpaceKey());
        }
        if (request.archived() != null) {
            if (request.archived()) {
                project.archive();
            } else {
                project.unarchive();
            }
        }
        // Flush so @UpdateTimestamp reflects this change in the returned DTO.
        return ProjectResponse.from(projectRepository.saveAndFlush(project));
    }

    // Scope every single-project lookup to the owner; a project owned by someone else is reported
    // as not-found rather than forbidden so we don't leak that the id exists.
    private Project requireOwned(UUID ownerId, UUID projectId) {
        return projectRepository.findById(projectId)
                .filter(project -> project.getOwner().getId().equals(ownerId))
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }
}
