package com.ks.bayyinah.infra.remote.dto.sync;

public record RemoteUserPreferenceUpdateRequest(
    String theme,
    Integer fontSize,
    Integer defaultTranslation,
    String language,
    String readingMode,
    Boolean showTransliteration,
    Boolean autoScroll) {
}
