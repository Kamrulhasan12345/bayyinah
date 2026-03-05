package com.ks.bayyinah.infra.remote.dto.auth;

public record RegistrationRequest(
    String username,
    String email,
    String password,
    String firstName,
    String lastName) {
}
