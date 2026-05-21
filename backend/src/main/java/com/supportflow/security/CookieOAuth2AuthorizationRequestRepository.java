package com.supportflow.security;

import com.supportflow.config.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Stores the in-flight {@link OAuth2AuthorizationRequest} in a short-lived, HttpOnly cookie
 * instead of the HTTP session, so the security filter chain can stay STATELESS and the OAuth
 * handshake survives across Cloud Run instances.
 *
 * <p>The serialized request is HMAC-signed with the app's JWT secret and the signature is
 * verified before deserialization — we only ever deserialize blobs this server produced, which
 * removes the Java-deserialization-gadget risk of trusting raw cookie bytes.
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_MAX_AGE_SECONDS = 180;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] signingKey;

    public CookieOAuth2AuthorizationRequestRepository(JwtProperties jwtProperties) {
        this.signingKey = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request).map(this::deserialize).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response, request.isSecure());
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, serialize(authorizationRequest))
                .path("/")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .maxAge(COOKIE_MAX_AGE_SECONDS)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            deleteCookie(response, request.isSecure());
        }
        return authorizationRequest;
    }

    private java.util.Optional<Cookie> readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .findFirst();
    }

    private void deleteCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] payload = toBytes(authorizationRequest);
        byte[] signature = sign(payload);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(payload) + "." + encoder.encodeToString(signature);
    }

    private OAuth2AuthorizationRequest deserialize(Cookie cookie) {
        String[] parts = cookie.getValue().split("\\.", 2);
        if (parts.length != 2) {
            return null;
        }
        Base64.Decoder decoder = Base64.getUrlDecoder();
        byte[] payload;
        byte[] signature;
        try {
            payload = decoder.decode(parts[0]);
            signature = decoder.decode(parts[1]);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (!MessageDigest.isEqual(signature, sign(payload))) {
            return null;
        }
        return fromBytes(payload);
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign OAuth2 authorization request cookie", ex);
        }
    }

    private byte[] toBytes(OAuth2AuthorizationRequest authorizationRequest) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(authorizationRequest);
            out.flush();
            return bytes.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize OAuth2 authorization request", ex);
        }
    }

    private OAuth2AuthorizationRequest fromBytes(byte[] payload) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            return (OAuth2AuthorizationRequest) in.readObject();
        } catch (Exception ex) {
            return null;
        }
    }
}
