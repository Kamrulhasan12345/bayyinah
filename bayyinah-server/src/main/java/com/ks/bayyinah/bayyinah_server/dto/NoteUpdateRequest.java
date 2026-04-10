package com.ks.bayyinah.bayyinah_server.dto;

public record NoteUpdateRequest(
    String content,
    Boolean isPrivate) {
}