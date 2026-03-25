package com.ks.bayyinah.infra.remote.dto.sync;

public record RemoteBookmarkUpsertRequest(
    Integer surahNumber,
    Integer ayahNumber,
    String title,
    String color) {
}
