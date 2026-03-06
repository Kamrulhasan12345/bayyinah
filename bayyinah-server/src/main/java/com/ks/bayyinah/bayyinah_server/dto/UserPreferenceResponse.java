package com.ks.bayyinah.bayyinah_server.dto;

import com.ks.bayyinah.bayyinah_server.model.UserPreference;

public record UserPreferenceResponse(
    String message,
    UserPreference userPreference) {
}
