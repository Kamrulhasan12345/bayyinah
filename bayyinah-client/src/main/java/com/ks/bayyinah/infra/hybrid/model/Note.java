package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Note {

  private Long id;
  private Long serverId;
  private int surahNumber;
  private int ayahNumber;
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean synced;
  private boolean deleted;

  // Constructors
  public Note(int surahNumber, int ayahNumber, String content) {
    this.surahNumber = surahNumber;
    this.ayahNumber = ayahNumber;
    this.content = content;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.synced = false;
    this.deleted = false;
  }

  // Getters and Setters
  public void setContent(String content) {
    this.content = content;
    this.updatedAt = LocalDateTime.now();
    this.synced = false; // Mark as needing sync after edit
  }

  public String getVerseKey() {
    return surahNumber + ":" + ayahNumber;
  }

  public boolean needsSync() {
    return !synced;
  }

  /**
   * Get preview of note content (first 50 chars)
   */
  public String getPreview() {
    if (content == null)
      return "";
    return content.length() > 50
        ? content.substring(0, 47) + "..."
        : content;
  }
}
