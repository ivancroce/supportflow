package com.supportflow.security;

import com.supportflow.config.JwtProperties;
import com.supportflow.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    /** HS256 requires at least 256 bits (32 bytes) of key material. */
    private static final int MIN_SECRET_BYTES = 32;
    /** Tolerate small clock drift on token expiry checks. */
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final SecretKey key;
    private final JwtParser parser;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            // Fail fast at startup with a clear message rather than a cryptic
            // io.jsonwebtoken.security.WeakKeyException on the first request that tries to sign.
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256; got " + secretBytes.length + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build();
        this.expirationMs = properties.expirationMs();
    }

    /**
     * Mint a SupportFlow access token. The subject ({@code sub}) is the user's UUID — the only
     * identifier we trust on the server. Email/name are convenience claims for the SPA and must not
     * be used for authorization on the server (always look up the user via the subject).
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public Claims parse(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
