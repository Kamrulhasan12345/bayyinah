package com.ks.bayyinah.bayyinah_server.dto;

import com.ks.bayyinah.bayyinah_server.model.User;

public record RegistrationResponse(
    String message, User user) {
}
