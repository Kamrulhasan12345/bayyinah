package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bookmark {

  private Long id; // Local SQLite ID
  private Long serverId; // PostgreSQL ID (null until synced)
  private int surahNumber;
  private int ayahNumber;
  private String title;
  private String color; // Hex color, e.g., "#FFD700"
  private LocalDateTime createdAt;
  private boolean synced; // Has this been uploaded to server?
  private boolean deleted; // Soft delete flag

  // Constructors
  public Bookmark(int surahNumber, int ayahNumber, String title) {
    this.surahNumber = surahNumber;
    this.ayahNumber = ayahNumber;
    this.title = title;
    this.color = "#FFD700"; // Default gold
    this.createdAt = LocalDateTime.now();
    this.synced = false;
    this.deleted = false;
  }

  /**
   * Get verse reference string (e.g., "2:255")
   */
  public String getVerseKey() {
    return surahNumber + ":" + ayahNumber;
  }

  /**
   * Check if this bookmark needs sync
   */
  public boolean needsSync() {
    return !synced;
  }
}
