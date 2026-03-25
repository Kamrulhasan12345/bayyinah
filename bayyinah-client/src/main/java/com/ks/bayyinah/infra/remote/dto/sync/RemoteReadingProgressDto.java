package com.ks.bayyinah.infra.remote.dto.sync;

import java.time.LocalDateTime;

public record RemoteReadingProgressDto(
    Long id,
    Integer surahNumber,
    Integer ayahNumber,
    LocalDateTime lastReadAt,
    Float completionPercentage,
    Integer totalReadTimeMinutes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long userId) {
}
