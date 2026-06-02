package com.supportflow.ai;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AiAdvisorApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private FakeGeminiClient fakeGeminiClient;

    @BeforeEach
    void resetFake() {
        fakeGeminiClient.reset();
    }

    @Test
    void firstGetGeneratesAndCachesSuggestion() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "advisor-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme shop", "Acme", "E-commerce site", null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "Checkout fails on Safari");

        fakeGeminiClient.willReturn(new GeminiSuggestion(
                "BUG", "Checkout", "HIGH",
                "Restated problem: checkout breaks on Safari.\nSuspected area: payment flow.\nSeverity: high.\nMissing info: Safari version."));

        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.suggestion.suggestedType").value("BUG"))
                .andExpect(jsonPath("$.suggestion.suggestedCategory").value("Checkout"))
                .andExpect(jsonPath("$.suggestion.suggestedPriority").value("HIGH"))
                .andExpect(jsonPath("$.suggestion.draftNote").isNotEmpty())
                .andExpect(jsonPath("$.suggestion.generatedAt").isNotEmpty());
    }

    @Test
    void secondGetReturnsCachedSuggestionWithoutCallingGeminiAgain() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "cache-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "subject");

        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        fakeGeminiClient.willReturn(new GeminiSuggestion("BUG", "Other", "URGENT", "should not appear"));

        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.suggestion.suggestedType").value("QUESTION"))
                .andExpect(jsonPath("$.suggestion.draftNote").value("Default fake triage note."));

        // Gemini called exactly once across the two GETs.
        org.junit.jupiter.api.Assertions.assertEquals(1, fakeGeminiClient.callCount());
    }

    @Test
    void genericGeminiFailureReturnsFailedEnvelopeAndDoesNotCache() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "fail-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "subject");

        fakeGeminiClient.willThrow(new GeminiException("boom"));

        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error").value("GENERATION_FAILED"))
                .andExpect(jsonPath("$.suggestion").doesNotExist());

        // Recovery: next call (after we clear the failure) generates fresh — proving nothing was cached.
        fakeGeminiClient.willReturn(new GeminiSuggestion("BUG", "Login", "HIGH", "ok now"));
        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.suggestion.suggestedType").value("BUG"));
    }

    @Test
    void quotaErrorIsDistinguishedInTheEnvelope() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "quota-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "subject");

        fakeGeminiClient.willThrow(new GeminiQuotaException("free tier exhausted"));

        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error").value("QUOTA"));
    }

    @Test
    void cannotGetSuggestionForAnotherUsersTicket() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "a-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "b-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(userA.getId(), project.id(), "private");

        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(userB.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void regenerateOverwritesAnExistingCachedSuggestion() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "regen-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "subject");

        // First GET seeds the cache with default Fake values.
        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(jsonPath("$.suggestion.suggestedType").value("QUESTION"));

        // Reconfigure the Fake, then force a regenerate.
        fakeGeminiClient.willReturn(new GeminiSuggestion("BUG", "Auth", "URGENT", "fresh note"));

        mockMvc.perform(post("/api/tickets/{id}/ai-suggestion/regenerate", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.suggestion.suggestedType").value("BUG"))
                .andExpect(jsonPath("$.suggestion.suggestedPriority").value("URGENT"))
                .andExpect(jsonPath("$.suggestion.draftNote").value("fresh note"));

        // Subsequent GET returns the overwritten suggestion from cache.
        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(jsonPath("$.suggestion.suggestedType").value("BUG"));

        org.junit.jupiter.api.Assertions.assertEquals(2, fakeGeminiClient.callCount());
    }

    @Test
    void regenerateOnAFreshTicketCreatesTheFirstCacheRow() throws Exception {
        User owner = userService.findOrCreateOAuthUser(
                "fresh-" + UUID.randomUUID() + "@example.com", "Owner", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                owner.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(owner.getId(), project.id(), "subject");

        fakeGeminiClient.willReturn(new GeminiSuggestion("FEATURE_REQUEST", "UX", "LOW", "first call"));

        mockMvc.perform(post("/api/tickets/{id}/ai-suggestion/regenerate", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.suggestion.suggestedType").value("FEATURE_REQUEST"));

        // Subsequent GET sees the cached row without an extra Gemini call.
        mockMvc.perform(get("/api/tickets/{id}/ai-suggestion", ticketId)
                        .with(authentication(authAs(owner.getId()))))
                .andExpect(jsonPath("$.suggestion.suggestedType").value("FEATURE_REQUEST"));
        org.junit.jupiter.api.Assertions.assertEquals(1, fakeGeminiClient.callCount());
    }

    @Test
    void cannotRegenerateForAnotherUsersTicket() throws Exception {
        User userA = userService.findOrCreateOAuthUser(
                "ra-" + UUID.randomUUID() + "@example.com", "A", null, AuthProvider.GOOGLE);
        User userB = userService.findOrCreateOAuthUser(
                "rb-" + UUID.randomUUID() + "@example.com", "B", null, AuthProvider.GOOGLE);
        ProjectResponse project = projectService.create(
                userA.getId(),
                new CreateProjectRequest("Acme", null, null, null, null, null));
        UUID ticketId = createTicket(userA.getId(), project.id(), "private");

        mockMvc.perform(post("/api/tickets/{id}/ai-suggestion/regenerate", ticketId)
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
