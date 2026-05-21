package com.supportflow.auth;

import com.supportflow.auth.dto.DevLoginRequest;
import com.supportflow.auth.dto.TokenResponse;
import com.supportflow.config.AuthProperties;
import com.supportflow.entity.User;
import com.supportflow.entity.enums.AuthProvider;
import com.supportflow.repository.UserRepository;
import com.supportflow.security.JwtService;
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

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    public DevAuthController(
            UserRepository userRepository, JwtService jwtService, AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody DevLoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        if (!authProperties.isAllowed(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email is not on the allowlist");
        }
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(email, null, null, AuthProvider.DEV)));
        return new TokenResponse(jwtService.generateToken(user));
    }
}
