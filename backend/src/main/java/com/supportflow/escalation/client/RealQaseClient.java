package com.supportflow.escalation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Production Qase client. Creates a test case via {@code POST /v1/case/{projectCode}} authenticated
 * with the {@code Token} header. The numeric case id comes back under {@code result.id}; the web URL
 * is constructed from the project code + id since the create response doesn't return one.
 *
 * <p>Not retried — case creation is not idempotent (see {@link RealJiraClient}). Failures surface as
 * {@link QaseException}.
 */
@Component
@ConditionalOnProperty(name = "supportflow.integrations.client", havingValue = "real", matchIfMissing = true)
public class RealQaseClient implements QaseClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient http;
    private final String baseUrl;
    private final String appUrl;
    private final String apiToken;

    public RealQaseClient(
            RestClient.Builder builder,
            @Value("${supportflow.integrations.qase.base-url:https://api.qase.io/v1}") String baseUrl,
            @Value("${supportflow.integrations.qase.app-url:https://app.qase.io}") String appUrl,
            @Value("${supportflow.integrations.qase.api-token:}") String apiToken) {
        this.http = builder.build();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.appUrl = stripTrailingSlash(appUrl);
        this.apiToken = apiToken;
    }

    @Override
    public QaseCase createCase(QaseCaseRequest request) {
        Map<String, Object> body = Map.of(
                "title", request.title(),
                "description", request.description() == null ? "" : request.description());
        try {
            String response = http.post()
                    .uri(baseUrl + "/case/" + request.projectCode())
                    .header("Token", apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            String id = readId(response);
            // Qase has no create-time web URL; build the case deep-link from code + id.
            String url = appUrl + "/case/" + request.projectCode() + "-" + id;
            return new QaseCase(id, url);
        } catch (RestClientResponseException e) {
            throw new QaseException("Qase case creation failed: " + e.getStatusCode() + " "
                    + e.getResponseBodyAsString(), e);
        } catch (RuntimeException e) {
            throw new QaseException("Qase case creation failed: " + e.getMessage(), e);
        }
    }

    private static String readId(String response) {
        try {
            JsonNode id = MAPPER.readTree(response).path("result").path("id");
            if (id.isMissingNode() || !id.isNumber()) {
                throw new QaseException("Qase response missing result.id: " + response);
            }
            return id.asText();
        } catch (QaseException e) {
            throw e;
        } catch (Exception e) {
            throw new QaseException("Could not parse Qase response: " + e.getMessage(), e);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
