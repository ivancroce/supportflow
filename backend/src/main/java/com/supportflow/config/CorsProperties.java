package com.supportflow.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allowed browser origins for the SPA. In local dev this is the Vite server; in production it is the
 * deployed frontend (Vercel). Driven by the {@code FRONTEND_ORIGIN} env var (comma-separated for more
 * than one) so the same build serves any environment without code changes.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
