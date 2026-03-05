package com.ks.bayyinah.infra.remote.dto.auth;

public record LoginResponse(String message, UserResponse user, TokensResponse tokens) {
}
