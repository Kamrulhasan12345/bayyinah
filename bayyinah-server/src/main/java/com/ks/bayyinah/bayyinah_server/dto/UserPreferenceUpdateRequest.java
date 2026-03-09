package com.ks.bayyinah.bayyinah_server.dto;

public record UserPreferenceUpdateRequest(
    String theme,
    Integer fontSize,
    Integer defaultTranslation,
    String language,
    String readingMode,
    Boolean showTransliteration,
    Boolean autoScroll) {
}
