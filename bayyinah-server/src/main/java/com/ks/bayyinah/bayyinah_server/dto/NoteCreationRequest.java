package com.ks.bayyinah.bayyinah_server.dto;

public record NoteCreationRequest(
    Integer surahNumber,
    Integer ayahNumber,
    String content,
    Boolean isPrivate) {
}