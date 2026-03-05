package com.ks.bayyinah.infra.remote.dto.auth;

public record RegistrationResponse(
    String message, UserResponse user) {
}
