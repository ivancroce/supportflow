package com.supportflow.ticket;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.project.ProjectService;
import com.supportflow.project.dto.CreateProjectRequest;
import com.supportflow.project.dto.ProjectResponse;
import com.supportflow.user.AuthProvider;
import com.supportflow.user.User;
import com.supportflow.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    @PersistenceContext private EntityManager em;

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

    @Test
    void listsTicketsInProject() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "list-" + UUID.randomUUID() + "@example.com", "List", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        createTicket(owner.getId(), project.id(), "first");
        createTicket(owner.getId(), project.id(), "second");

        mockMvc.perform(get("/api/projects/{id}/tickets", project.id())
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].projectId").value(project.id().toString()));
    }

    @Test
    void deletesTicket() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "del-" + UUID.randomUUID() + "@example.com", "Del", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "to delete");

        mockMvc.perform(delete("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingProjectCascadesToItsTickets() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "casc-" + UUID.randomUUID() + "@example.com", "Casc", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Doomed", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "casualty");

        mockMvc.perform(delete("/api/projects/{id}", project.id())
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isNoContent());

        // Force the pending project DELETE to actually hit the DB (so the FK CASCADE fires) and
        // drop any cached Ticket from the persistence context. Real prod doesn't need this — each
        // HTTP request gets its own transaction — but @Transactional MockMvc tests share one.
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getsSingleTicket() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "get-" + UUID.randomUUID() + "@example.com", "Get", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "hello");

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.subject").value("hello"));
    }

    @Test
    void patchesTicketStatusAndPriority() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "patch-" + UUID.randomUUID() + "@example.com", "Patch", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "needs triage");

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(owner.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS",
                                  "priority": "HIGH",
                                  "category": "billing"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.category").value("billing"))
                .andExpect(jsonPath("$.subject").value("needs triage"));
    }

    @Test
    void cannotGetAnotherUsersTicket() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(userA.getId(), project.id(), "private");

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(userB.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotPatchAnotherUsersTicket() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(userA.getId(), project.id(), "private");

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(userB.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteAnotherUsersTicket() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(userA.getId(), project.id(), "private");

        mockMvc.perform(delete("/api/tickets/{id}", ticketId)
                        .with(authentication(authAs(userB.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotListAnotherUsersProjectTickets() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        createTicket(userA.getId(), project.id(), "private");

        mockMvc.perform(get("/api/projects/{id}/tickets", project.id())
                        .with(authentication(authAs(userB.getId()))))
                .andExpect(status().isNotFound());
    }

    private UUID createTicket(UUID ownerId, UUID projectId, String subject) throws Exception {
        String body = mockMvc.perform(post("/api/projects/{id}/tickets", projectId)
                        .with(authentication(authAs(ownerId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"" + subject + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.split("\"id\":\"")[1].split("\"")[0]);
    }

    private static UsernamePasswordAuthenticationToken authAs(UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
