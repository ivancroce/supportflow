package com.supportflow.auth;

import com.supportflow.auth.dto.DevLoginRequest;
import com.supportflow.auth.dto.TokenResponse;
import com.supportflow.config.AuthProperties;
import com.supportflow.security.JwtService;
import com.supportflow.user.User;
import com.supportflow.user.UserService;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dev")
@Profile("local")
public class DevAuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    public DevAuthController(
            UserService userService, JwtService jwtService, AuthProperties authProperties) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody DevLoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        if (!authProperties.isAllowed(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email is not on the allowlist");
        }
        User user = userService.findOrCreateDevUser(email);
        return new TokenResponse(jwtService.generateToken(user));
    }
}
