package com.supportflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportflow.ticket.TicketPriority;
import com.supportflow.ticket.TicketType;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Production Gemini client. Uses Gemini's `responseSchema` to constrain enums at the model boundary
 * (see grilling Q4); failures are surfaced as {@link GeminiQuotaException} on HTTP 429 and
 * {@link GeminiException} otherwise so the service can return distinct envelope errors.
 *
 * <p>The API key is sent as the {@code x-goog-api-key} header rather than a {@code ?key=} query
 * param so it can never end up in a URL-bearing log line (request logs, exception messages).
 *
 * <p>Schema enum literals are derived from {@link TicketType} / {@link TicketPriority} so a new
 * enum value automatically propagates to the prompt. The {@code responseSchema} constraints are
 * best-effort hints, not hard guarantees, so {@link #parseSuggestion} additionally validates the
 * enum fields and truncates {@code suggestedCategory} to the {@code ai_suggestions.suggested_category}
 * column width — an over-long or out-of-enum value would otherwise escape as a JPA constraint
 * violation (a 500) instead of a clean {@code GENERATION_FAILED} envelope.
 */
@Component
@ConditionalOnProperty(name = "supportflow.ai.client", havingValue = "real", matchIfMissing = true)
public class RealGeminiClient implements GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(RealGeminiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int CATEGORY_MAX_LENGTH = 128;

    private static final Map<String, Object> RESPONSE_SCHEMA = buildResponseSchema();

    private final RestClient http;
    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public RealGeminiClient(
            RestClient.Builder builder,
            @Value("${supportflow.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${supportflow.ai.gemini.model:gemini-2.5-flash}") String model,
            @Value("${supportflow.ai.gemini.api-key:}") String apiKey) {
        this.http = builder.build();
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
    }

    @PostConstruct
    void warnIfApiKeyMissing() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("supportflow.ai.gemini.api-key is empty — every AI suggestion call will fail. "
                    + "Set GEMINI_API_KEY in the environment.");
        }
    }

    @Override
    public GeminiSuggestion classify(GeminiInput input) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", buildPrompt(input))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RESPONSE_SCHEMA));

        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("models", model + ":generateContent")
                .build()
                .toUriString();

        String responseBody;
        try {
            responseBody = http.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 429) {
                throw new GeminiQuotaException("Gemini quota exhausted: " + e.getResponseBodyAsString());
            }
            throw new GeminiException("Gemini call failed: " + status + " " + e.getResponseBodyAsString(), e);
        } catch (RuntimeException e) {
            throw new GeminiException("Gemini call failed: " + e.getMessage(), e);
        }

        return parseSuggestion(responseBody);
    }

    private static GeminiSuggestion parseSuggestion(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            String modelJson = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText(null);
            if (modelJson == null) {
                throw new GeminiException("Gemini response missing candidates[0].content.parts[0].text");
            }
            JsonNode s = MAPPER.readTree(modelJson);
            return new GeminiSuggestion(
                    requireEnum(s, "suggestedType", enumNames(TicketType.class)),
                    truncate(requireField(s, "suggestedCategory"), CATEGORY_MAX_LENGTH),
                    requireEnum(s, "suggestedPriority", enumNames(TicketPriority.class)),
                    requireField(s, "draftNote"));
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Could not parse Gemini response: " + e.getMessage(), e);
        }
    }

    private static String requireField(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null) {
            throw new GeminiException("Gemini response missing required field: " + field);
        }
        return value;
    }

    /** Validate against the enum allow-list — the schema is best-effort, so a hallucinated literal here
     * is treated as a generation failure rather than persisted as a garbage type/priority. */
    private static String requireEnum(JsonNode node, String field, List<String> allowed) {
        String value = requireField(node, field);
        if (!allowed.contains(value)) {
            throw new GeminiException("Gemini returned out-of-enum value for " + field + ": " + value);
        }
        return value;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String buildPrompt(GeminiInput input) {
        StringBuilder p = new StringBuilder();
        p.append("You are a support-ticket triage assistant. Classify the ticket and draft a triage note. ")
                .append("Do NOT diagnose the bug or propose a fix.\n\n");
        if (input.projectName() != null) {
            p.append("Project: ").append(input.projectName());
            if (input.projectClientName() != null) p.append(" (client: ").append(input.projectClientName()).append(")");
            p.append("\n");
        }
        if (input.projectDescription() != null && !input.projectDescription().isBlank()) {
            p.append("Project context: ").append(input.projectDescription()).append("\n");
        }
        p.append("\nTicket subject: ").append(input.ticketSubject()).append("\n");
        if (input.ticketDescription() != null && !input.ticketDescription().isBlank()) {
            p.append("Ticket description: ").append(input.ticketDescription()).append("\n");
        }
        p.append("\nReturn JSON with: suggestedType (")
                .append(String.join("|", enumNames(TicketType.class)))
                .append("), suggestedCategory (short free-text label, max ")
                .append(CATEGORY_MAX_LENGTH).append(" chars), suggestedPriority (")
                .append(String.join("|", enumNames(TicketPriority.class)))
                .append("), draftNote (a short structured summary: restated problem, ")
                .append("suspected area/severity, missing info).");
        return p.toString();
    }

    private static Map<String, Object> buildResponseSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("suggestedType", Map.of(
                "type", "string",
                "enum", enumNames(TicketType.class)));
        properties.put("suggestedCategory", Map.of(
                "type", "string",
                "maxLength", CATEGORY_MAX_LENGTH));
        properties.put("suggestedPriority", Map.of(
                "type", "string",
                "enum", enumNames(TicketPriority.class)));
        properties.put("draftNote", Map.of("type", "string"));
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("suggestedType", "suggestedCategory", "suggestedPriority", "draftNote"));
    }

    private static <E extends Enum<E>> List<String> enumNames(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).toList();
    }
}
