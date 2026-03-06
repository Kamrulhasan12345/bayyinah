package com.ks.bayyinah.bayyinah_server.dto;

public record BookmarkCreationRequest(
    Integer surahNumber,
    Integer ayahNumber,
    String title,
    String color) {
}
