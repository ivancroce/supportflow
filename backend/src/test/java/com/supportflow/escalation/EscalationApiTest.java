package com.supportflow.escalation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.escalation.client.ConfluenceException;
import com.supportflow.escalation.client.FakeConfluenceClient;
import com.supportflow.escalation.client.FakeJiraClient;
import com.supportflow.escalation.client.FakeQaseClient;
import com.supportflow.ai.FakeGeminiClient;
import com.supportflow.project.ProjectService;
import com.supportflow.project.dto.CreateProjectRequest;
import com.supportflow.project.dto.ProjectResponse;
import com.supportflow.user.AuthProvider;
import com.supportflow.user.User;
import com.supportflow.user.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EscalationApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private FakeJiraClient fakeJira;
    @Autowired private FakeQaseClient fakeQase;
    @Autowired private FakeConfluenceClient fakeConfluence;
    @Autowired private FakeGeminiClient fakeGemini;

    @BeforeEach
    void resetFakes() {
        fakeJira.reset();
        fakeQase.reset();
        fakeConfluence.reset();
        fakeGemini.reset();
    }

    @Test
    void escalateCreatesAllThreeRecordsAndMarksTicketAnEscalatedBug() throws Exception {
        UUID owner = ownerId("escalate");
        UUID projectId = configuredProject(owner);
        UUID ticketId = createTicket(owner, projectId, "Checkout fails on Safari");

        mockMvc.perform(post("/api/tickets/{id}/escalate", ticketId)
                        .with(authentication(authAs(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.targets[0].tool").value("JIRA"))
                .andExpect(jsonPath("$.targets[0].outcome").value("CREATED"))
                .andExpect(jsonPath("$.targets[0].key").value("SFJ-1"))
                .andExpect(jsonPath("$.targets[1].tool").value("QASE"))
                .andExpect(jsonPath("$.targets[1].outcome").value("CREATED"))
                .andExpect(jsonPath("$.targets[2].tool").value("CONFLUENCE"))
                .andExpect(jsonPath("$.targets[2].outcome").value("CREATED"))
                .andExpect(jsonPath("$.ticket.escalated").value(true))
                .andExpect(jsonPath("$.ticket.type").value("BUG"))
                .andExpect(jsonPath("$.ticket.jiraIssueKey").value("SFJ-1"))
                .andExpect(jsonPath("$.ticket.qaseCaseId").value("1"))
                .andExpect(jsonPath("$.ticket.confluencePageId").value("655361"));
    }

    @Test
    void partialFailureKeepsTheSucceededToolsAndLeavesTicketNotFullyEscalated() throws Exception {
        UUID owner = ownerId("partial");
        UUID projectId = configuredProject(owner);
        UUID ticketId = createTicket(owner, projectId, "Login broken");

        fakeConfluence.willThrow(new ConfluenceException("Confluence 503"));

        mockMvc.perform(post("/api/tickets/{id}/escalate", ticketId)
                        .with(authentication(authAs(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.targets[0].outcome").value("CREATED"))
                .andExpect(jsonPath("$.targets[1].outcome").value("CREATED"))
                .andExpect(jsonPath("$.targets[2].outcome").value("FAILED"))
                .andExpect(jsonPath("$.targets[2].error").isNotEmpty())
                // Jira/Qase ids are persisted; the ticket is a bug but not fully escalated yet.
                .andExpect(jsonPath("$.ticket.escalated").value(false))
                .andExpect(jsonPath("$.ticket.type").value("BUG"))
                .andExpect(jsonPath("$.ticket.jiraIssueKey").value("SFJ-1"))
                .andExpect(jsonPath("$.ticket.qaseCaseId").value("1"))
                .andExpect(jsonPath("$.ticket.confluencePageId").doesNotExist());
    }

    @Test
    void retrySkipsAlreadyLinkedToolsAndCompletesTheEscalation() throws Exception {
        UUID owner = ownerId("retry");
        UUID projectId = configuredProject(owner);
        UUID ticketId = createTicket(owner, projectId, "Cart total wrong");

        // First attempt: Confluence fails -> partial, Jira + Qase created.
        fakeConfluence.willThrow(new ConfluenceException("Confluence down"));
        mockMvc.perform(post("/api/tickets/{id}/escalate", ticketId)
                        .with(authentication(authAs(owner))))
                .andExpect(jsonPath("$.status").value("PARTIAL"));

        // Recover Confluence and retry (without resetting call counts).
        fakeConfluence.willReturn(new com.supportflow.escalation.client.ConfluenceClient.ConfluencePage(
                "999", "https://wiki.example/pages/999"));

        mockMvc.perform(post("/api/tickets/{id}/escalate", ticketId)
                        .with(authentication(authAs(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.targets[0].outcome").value("ALREADY_LINKED"))
                .andExpect(jsonPath("$.targets[0].key").value("SFJ-1"))
                .andExpect(jsonPath("$.targets[1].outcome").value("ALREADY_LINKED"))
                .andExpect(jsonPath("$.targets[2].outcome").value("CREATED"))
                .andExpect(jsonPath("$.targets[2].key").value("999"))
                .andExpect(jsonPath("$.ticket.escalated").value(true))
                .andExpect(jsonPath("$.ticket.confluencePageId").value("999"));

        // The already-linked tools were not called a second time; only Confluence was retried.
        Assertions.assertEquals(1, fakeJira.callCount());
        Assertions.assertEquals(1, fakeQase.callCount());
        Assertions.assertEquals(2, fakeConfluence.callCount());
    }

    @Test
    void escalatingAProjectMissingATargetFailsFastWith422AndMakesNoCalls() throws Exception {
        UUID owner = ownerId("misconfigured");
        // Confluence space key omitted.
        ProjectResponse project = projectService.create(owner, new CreateProjectRequest(
                "Acme", "Acme", "desc", "SFJ", "SQT", null));
        UUID ticketId = createTicket(owner, project.id(), "Some bug");

        mockMvc.perform(post("/api/tickets/{id}/escalate", ticketId)
                        .with(authentication(authAs(owner))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.missingTargets[0]").value("confluenceSpaceKey"));

        Assertions.assertEquals(0, fakeJira.callCount());
        Assertions.assertEquals(0, fakeQase.callCount());
        Assertions.assertEquals(0, fakeConfluence.callCount());
    }

    @Test
    void cannotEscalateAnotherUsersTicket() throws Exception {
        UUID userA = ownerId("owner-a");
        UUID userB = ownerId("owner-b");
        UUID projectId = configuredProject(userA);
        UUID ticketId = createTicket(userA, projectId, "private");

        mockMvc.perform(post("/api/tickets/{id}/escalate", ticketId)
                        .with(authentication(authAs(userB))))
                .andExpect(status().isNotFound());

        Assertions.assertEquals(0, fakeJira.callCount());
    }

    @Test
    void cachedTriageNoteIsSeededIntoTheJiraAndConfluenceBodies() throws Exception {
        UUID owner = ownerId("triage");
        UUID projectId = configuredProject(owner);
        UUID ticketId = createTicket(owner, projectId, "Page 500s on submit");

        // Generate + cache the Triage Note (FakeGeminiClient returns a default note).
        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner))))
                .andExpect(jsonPath("$.status").value("READY"));

        mockMvc.perform(post("/api/tickets/{id}/escalate", ticketId)
                        .with(authentication(authAs(owner))))
                .andExpect(jsonPath("$.status").value("ESCALATED"));

        // The note made it into the bodies the clients received.
        Assertions.assertTrue(fakeJira.lastRequest().description().contains("Triage summary:"));
        Assertions.assertTrue(fakeJira.lastRequest().description().contains("Default fake triage note."));
        Assertions.assertTrue(fakeConfluence.lastRequest().bodyText().contains("Triage summary:"));
    }

    // --- helpers ---

    private UUID configuredProject(UUID owner) {
        return projectService.create(owner, new CreateProjectRequest(
                "Acme", "Acme", "E-commerce site", "SFJ", "SQT", "ST")).id();
    }

    private UUID ownerId(String label) {
        User user = userService.findOrCreateOAuthUser(
                label + "-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        return user.getId();
    }

    private UUID createTicket(UUID ownerId, UUID projectId, String subject) throws Exception {
        String body = mockMvc.perform(post("/api/projects/{id}/tickets", projectId)
                        .with(authentication(authAs(ownerId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"" + subject + "\",\"description\":\"It is broken.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.split("\"id\":\"")[1].split("\"")[0]);
    }

    private static UsernamePasswordAuthenticationToken authAs(UUID userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
