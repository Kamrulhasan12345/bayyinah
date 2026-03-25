package com.ks.bayyinah.infra.remote.dto.sync;

import java.time.LocalDateTime;

public record RemoteUserPreferenceDto(
    String theme,
    Integer fontSize,
    Integer defaultTranslation,
    String language,
    String readingMode,
    Boolean showTransliteration,
    Boolean autoScroll,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long userId) {
}
