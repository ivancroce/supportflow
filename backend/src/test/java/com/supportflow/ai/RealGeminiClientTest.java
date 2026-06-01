package com.supportflow.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealGeminiClientTest {

    private static final String BASE_URL = "https://example.test/v1beta";
    private static final String MODEL = "gemini-2.5-flash";
    private static final String API_KEY = "test-key";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private RealGeminiClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RealGeminiClient(builder, BASE_URL, MODEL, API_KEY);
    }

    @Test
    void sendsCorrectlyShapedRequestAndParsesGeminiResponse() {
        // Gemini wraps the JSON the model returned inside candidates[0].content.parts[0].text.
        String modelJson = """
                {"suggestedType":"BUG","suggestedCategory":"Checkout",
                 "suggestedPriority":"HIGH","draftNote":"a triage note"}""";
        String geminiResponse = """
                {
                  "candidates": [
                    { "content": { "parts": [ { "text": %s } ] } }
                  ]
                }
                """.formatted(quoteJson(modelJson));

        server.expect(requestTo(BASE_URL + "/models/" + MODEL + ":generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(jsonPath("$.contents[0].parts[0].text").exists())
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.suggestedType.enum[0]").value("QUESTION"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.suggestedPriority.enum[0]").value("LOW"))
                .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));

        GeminiSuggestion s = client.classify(new GeminiInput(
                "Checkout fails", "Stuck on payment", "Acme shop", "Acme", "E-commerce site"));

        assertThat(s.suggestedType()).isEqualTo("BUG");
        assertThat(s.suggestedCategory()).isEqualTo("Checkout");
        assertThat(s.suggestedPriority()).isEqualTo("HIGH");
        assertThat(s.draftNote()).isEqualTo("a triage note");
        server.verify();
    }

    @Test
    void maps429ToQuotaException() {
        server.expect(requestTo(BASE_URL + "/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":\"quota\"}"));

        assertThatThrownBy(() -> client.classify(new GeminiInput("s", "d", "p", null, null)))
                .isInstanceOf(GeminiQuotaException.class);
    }

    @Test
    void mapsServerErrorToGenericGeminiException() {
        server.expect(requestTo(BASE_URL + "/models/" + MODEL + ":generateContent"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"oops\"}"));

        assertThatThrownBy(() -> client.classify(new GeminiInput("s", "d", "p", null, null)))
                .isInstanceOf(GeminiException.class)
                .isNotInstanceOf(GeminiQuotaException.class);
    }

    @Test
    void rejectsOutOfEnumTypeAsGeminiException() {
        // The responseSchema enum is best-effort; if the model returns a literal outside the enum
        // we fail the generation rather than persist a garbage type.
        String modelJson = """
                {"suggestedType":"NOT_A_TYPE","suggestedCategory":"X",
                 "suggestedPriority":"HIGH","draftNote":"n"}""";
        respondWithModelJson(modelJson);

        assertThatThrownBy(() -> client.classify(new GeminiInput("s", "d", "p", null, null)))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("suggestedType");
    }

    @Test
    void truncatesOverlongCategoryToColumnWidth() {
        String longCategory = "a".repeat(200);
        String modelJson = """
                {"suggestedType":"BUG","suggestedCategory":"%s",
                 "suggestedPriority":"HIGH","draftNote":"n"}""".formatted(longCategory);
        respondWithModelJson(modelJson);

        GeminiSuggestion s = client.classify(new GeminiInput("s", "d", "p", null, null));

        assertThat(s.suggestedCategory()).hasSize(128);
    }

    private void respondWithModelJson(String modelJson) {
        String geminiResponse = """
                { "candidates": [ { "content": { "parts": [ { "text": %s } ] } } ] }
                """.formatted(quoteJson(modelJson));
        server.expect(requestTo(BASE_URL + "/models/" + MODEL + ":generateContent"))
                .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));
    }

    /** Embed a JSON document as a JSON string literal (escapes quotes and newlines). */
    private static String quoteJson(String raw) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.append("\"").toString();
    }
}
