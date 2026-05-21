package com.supportflow.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(List<String> allowlist) {

    public boolean isAllowed(String email) {
        return allowlist != null
                && allowlist.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(email));
    }
}
