package com.supportflow.auth.dto;

import com.supportflow.user.AuthProvider;
import java.util.UUID;

public record MeResponse(UUID id, String email, String name, String avatarUrl, AuthProvider provider) {
}
