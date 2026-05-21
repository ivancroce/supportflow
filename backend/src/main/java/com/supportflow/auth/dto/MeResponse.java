package com.supportflow.auth.dto;

import com.supportflow.entity.enums.AuthProvider;
import java.util.UUID;

public record MeResponse(UUID id, String email, String name, String avatarUrl, AuthProvider provider) {
}
