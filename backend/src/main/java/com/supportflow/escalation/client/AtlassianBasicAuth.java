package com.supportflow.escalation.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Atlassian Cloud REST auth is HTTP Basic with {@code email:apiToken} (shared by Jira and
 * Confluence). Centralised so both real clients build the header the same way.
 */
final class AtlassianBasicAuth {

    private AtlassianBasicAuth() {}

    static String header(String email, String apiToken) {
        String credentials = email + ":" + apiToken;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
