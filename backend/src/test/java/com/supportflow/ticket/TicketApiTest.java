package com.supportflow.ticket;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.project.ProjectService;
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
class TicketApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;

    @Test
    void createsTicketWithDefaults() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "owner-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme site", "Acme", null, null, null, null));

        mockMvc.perform(post("/api/projects/{id}/tickets", project.id())
                        .with(authentication(authAs(owner.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Login button is broken",
                                  "description": "Clicking it does nothing"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.projectId").value(project.id().toString()))
                .andExpect(jsonPath("$.subject").value("Login button is broken"))
                .andExpect(jsonPath("$.description").value("Clicking it does nothing"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.type").value("QUESTION"))
                .andExpect(jsonPath("$.escalated").value(false));
    }

    private static UsernamePasswordAuthenticationToken authAs(UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
