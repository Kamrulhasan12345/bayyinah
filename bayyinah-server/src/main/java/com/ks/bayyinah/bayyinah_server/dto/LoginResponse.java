package com.ks.bayyinah.bayyinah_server.dto;

import com.ks.bayyinah.bayyinah_server.model.Tokens;
import com.ks.bayyinah.bayyinah_server.model.User;

public record LoginResponse(
    String message,
    User user,
    Tokens tokens) {
}
