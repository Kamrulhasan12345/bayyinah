package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReadingProgress {

  private Long id;
  private Long serverId;
  private int surahNumber;
  private int ayahNumber;
  private LocalDateTime lastReadAt;
  private int timeSpentSeconds; // Time spent on this verse
  private boolean synced;

  // Constructors
  public ReadingProgress(int surahNumber, int ayahNumber) {
    this.surahNumber = surahNumber;
    this.ayahNumber = ayahNumber;
    this.lastReadAt = LocalDateTime.now();
    this.timeSpentSeconds = 0;
    this.synced = false;
  }

  public String getVerseKey() {
    return surahNumber + ":" + ayahNumber;
  }

  /**
   * Add time spent and mark as needing sync
   */
  public void addTimeSpent(int seconds) {
    this.timeSpentSeconds += seconds;
    this.lastReadAt = LocalDateTime.now();
    this.synced = false;
  }

  /**
   * Update last read timestamp
   */
  public void touch() {
    this.lastReadAt = LocalDateTime.now();
    this.synced = false;
  }
}
