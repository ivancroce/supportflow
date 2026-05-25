package com.supportflow.auth;

import com.supportflow.auth.dto.MeResponse;
import com.supportflow.user.User;
import com.supportflow.user.UserService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal UUID userId) {
        User user = userService.getById(userId);
        return new MeResponse(
                user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(), user.getProvider());
    }
}
