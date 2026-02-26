package com.ks.bayyinah.bayyinah_server.dto;

public record RegistrationRequest(
    String username,
    String email,
    String password,
    String firstName,
    String lastName,
    String role) {
}
