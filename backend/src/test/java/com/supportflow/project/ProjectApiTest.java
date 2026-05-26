package com.supportflow.project;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.project.dto.CreateProjectRequest;
import com.supportflow.project.dto.ProjectResponse;
import com.supportflow.user.AuthProvider;
import com.supportflow.user.User;
import com.supportflow.user.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;

    @Test
    void cannotGetAnotherUsersProject() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));

        mockMvc.perform(get("/api/projects/{id}", project.id())
                        .with(authentication(authAs(userB.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotPatchAnotherUsersProject() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));

        mockMvc.perform(patch("/api/projects/{id}", project.id())
                        .with(authentication(authAs(userB.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hijacked\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteAnotherUsersProject() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));

        mockMvc.perform(delete("/api/projects/{id}", project.id())
                        .with(authentication(authAs(userB.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void listOnlyShowsOwnProjects() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));

        mockMvc.perform(get("/api/projects")
                        .with(authentication(authAs(userB.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private static UsernamePasswordAuthenticationToken authAs(UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
