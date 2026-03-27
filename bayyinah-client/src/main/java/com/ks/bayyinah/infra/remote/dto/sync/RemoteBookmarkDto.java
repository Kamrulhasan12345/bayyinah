package com.ks.bayyinah.infra.remote.dto.sync;

import java.time.LocalDateTime;

public record RemoteBookmarkDto(
    Long id,
    Integer surahNumber,
    Integer ayahNumber,
    String title,
    String color,
    LocalDateTime createdAt,
    Long userId) {
}
