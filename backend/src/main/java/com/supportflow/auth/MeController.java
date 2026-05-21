package com.supportflow.auth;

import com.supportflow.auth.dto.MeResponse;
import com.supportflow.entity.User;
import com.supportflow.exception.NotFoundException;
import com.supportflow.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public MeResponse me() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new MeResponse(
                user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(), user.getProvider());
    }
}
