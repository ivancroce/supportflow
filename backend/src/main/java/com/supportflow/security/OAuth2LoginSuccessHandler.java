package com.supportflow.security;

import com.supportflow.config.AuthProperties;
import com.supportflow.config.OAuthProperties;
import com.supportflow.entity.User;
import com.supportflow.entity.enums.AuthProvider;
import com.supportflow.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.client.RestClient;

/**
 * On a successful OAuth login: resolve the verified email, gate it against the allowlist,
 * find-or-create the {@link User}, mint an app JWT, and redirect the browser to the SPA with
 * the token in the URL fragment. Mirrors the dev-login path so there is one canonical way to
 * issue a SupportFlow token.
 */
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String GITHUB = "github";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthProperties authProperties;
    private final OAuthProperties oAuthProperties;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient = RestClient.create();

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            AuthProperties authProperties,
            OAuthProperties oAuthProperties,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
        this.oAuthProperties = oAuthProperties;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuth2User principal = token.getPrincipal();

        String email = resolveEmail(registrationId, principal, token);
        if (email == null || email.isBlank()) {
            redirectFailure(response, "email_unavailable");
            return;
        }
        email = email.toLowerCase(Locale.ROOT);

        if (!authProperties.isAllowed(email)) {
            redirectFailure(response, "not_allowed");
            return;
        }

        AuthProvider provider = GITHUB.equals(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE;
        String resolvedEmail = email;
        User user = userRepository.findByEmail(resolvedEmail)
                .orElseGet(() -> userRepository.save(new User(
                        resolvedEmail, resolveName(registrationId, principal),
                        resolveAvatar(registrationId, principal), provider)));

        response.sendRedirect(oAuthProperties.successRedirect() + "#token=" + jwtService.generateToken(user));
    }

    private void redirectFailure(HttpServletResponse response, String reason) throws IOException {
        response.sendRedirect(oAuthProperties.failureRedirect() + "?error=" + reason);
    }

    private String resolveEmail(String registrationId, OAuth2User principal, OAuth2AuthenticationToken token) {
        String email = principal.getAttribute("email");
        if (email != null || !GITHUB.equals(registrationId)) {
            return email;
        }
        // GitHub omits the email from the profile when the user keeps it private; fetch the
        // verified primary address from the dedicated endpoint using the granted access token.
        return fetchGitHubPrimaryEmail(token);
    }

    @SuppressWarnings("unchecked")
    private String fetchGitHubPrimaryEmail(OAuth2AuthenticationToken token) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(), token.getName());
        if (client == null) {
            return null;
        }
        List<Map<String, Object>> emails = restClient.get()
                .uri("https://api.github.com/user/emails")
                .header("Authorization", "Bearer " + client.getAccessToken().getTokenValue())
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(List.class);
        if (emails == null) {
            return null;
        }
        return emails.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                .map(e -> (String) e.get("email"))
                .findFirst()
                .orElse(null);
    }

    private String resolveName(String registrationId, OAuth2User principal) {
        String name = principal.getAttribute("name");
        if (name == null && GITHUB.equals(registrationId)) {
            return principal.getAttribute("login");
        }
        return name;
    }

    private String resolveAvatar(String registrationId, OAuth2User principal) {
        return GITHUB.equals(registrationId)
                ? principal.getAttribute("avatar_url")
                : principal.getAttribute("picture");
    }
}
