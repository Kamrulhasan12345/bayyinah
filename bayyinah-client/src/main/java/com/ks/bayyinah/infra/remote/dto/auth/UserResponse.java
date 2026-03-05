package com.ks.bayyinah.infra.remote.dto.auth;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    LocalDateTime createdAt) {
}
