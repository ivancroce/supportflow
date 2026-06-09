package com.supportflow.config;

import com.supportflow.security.CookieOAuth2AuthorizationRequestRepository;
import com.supportflow.security.JwtAuthenticationFilter;
import com.supportflow.security.JwtService;
import com.supportflow.security.OAuth2LoginSuccessHandler;
import com.supportflow.user.UserService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final Environment environment;
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthProperties authProperties;
    private final OAuthProperties oAuthProperties;
    private final CorsProperties corsProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
            Environment environment,
            UserService userService,
            JwtService jwtService,
            AuthProperties authProperties,
            OAuthProperties oAuthProperties,
            CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.environment = environment;
        this.userService = userService;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
        this.oAuthProperties = oAuthProperties;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService)
            throws Exception {

        // OAuth2 client auto-config only creates a ClientRegistrationRepository when at least one
        // provider has credentials. Without it, skip oauth2Login entirely so local dev still boots.
        boolean oauthEnabled = clientRegistrationRepository.getIfAvailable() != null;

        http
                // The SPA is served from a different origin (Vite in dev, Vercel in prod) than this
                // API, so the browser preflights cross-origin calls. Allow the configured origins.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF is disabled because this is a stateless API: clients send a Bearer JWT in
                // the Authorization header, not a session cookie, so the CSRF attack surface
                // (browser auto-attaching cookies to forged requests) doesn't apply.
                .csrf(csrf -> csrf.disable())
                // Defense in depth: explicitly disable the auth mechanisms we don't use, so they
                // can't be re-enabled by an accidental dependency or config change.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health").permitAll();
                    // The dev-login controller only exists under the "local" profile; whitelist its
                    // path there too so it can never be reachable in a deployed environment.
                    if (environment.matchesProfiles("local")) {
                        auth.requestMatchers("/api/dev/**").permitAll();
                    }
                    if (oauthEnabled) {
                        auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                // No/invalid token -> 401 (unauthenticated), not the default 403 from the
                // anonymous user failing authorization.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauthEnabled) {
            OAuth2LoginSuccessHandler successHandler = new OAuth2LoginSuccessHandler(
                    userService, jwtService, authProperties, oAuthProperties,
                    authorizedClientService.getObject());
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(endpoint ->
                            endpoint.authorizationRequestRepository(authorizationRequestRepository))
                    .successHandler(successHandler)
                    .failureHandler((request, response, exception) ->
                            response.sendRedirect(oAuthProperties.failureRedirect() + "?error=oauth_failed")));
        }

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // The SPA holds the JWT in localStorage and sends it as an Authorization header, not a
        // cookie, so credentialed CORS isn't needed; leaving it off keeps the policy strict.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
