package com.ks.bayyinah.infra.remote.dto.auth;

public record LoginRequest(
    String username,
    String password) {
}
