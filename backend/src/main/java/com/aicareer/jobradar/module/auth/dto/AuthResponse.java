package com.aicareer.jobradar.module.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UUID userId,
        String name,
        String email
) {
    public static AuthResponse of(String token, UUID userId, String name, String email) {
        return new AuthResponse(token, "Bearer", userId, name, email);
    }
}
