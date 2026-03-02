package com.ks.bayyinah.bayyinah_server.dto;

import com.ks.bayyinah.bayyinah_server.model.Tokens;

public record RefreshTokenResponse(
    String message,
    Tokens tokens) {
}
