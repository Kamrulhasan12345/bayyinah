package com.ks.bayyinah.infra.remote.dto.sync;

public record RemoteReadingProgressUpsertRequest(
    Integer surahNumber,
    Integer ayahNumber) {
}
