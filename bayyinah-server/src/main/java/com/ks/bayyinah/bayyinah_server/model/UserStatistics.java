package com.ks.bayyinah.bayyinah_server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

import jakarta.persistence.Column;

@Entity
@Table(name = "user_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatistics {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "total_reading_time_minutes", nullable = false)
  private Integer totalReadingTimeMinutes;

  @Column(name = "total_ayahs_read", nullable = false)
  private Integer totalAyahsRead;

  @Column(name = "total_surahs_completed", nullable = false)
  private Integer totalSurahsCompleted;

  @Column(name = "current_streak_days", nullable = false)
  private Integer currentStreakDays;

  @Column(name = "longest_streak_days", nullable = false)
  private Integer longestStreakDays;

  @Column(name = "last_streak_date", nullable = true)
  private LocalDateTime lastStreakDate;

  @Column(name = "total_bookmarks", nullable = false)
  private Integer totalBookmarks;

  @Column(name = "total_notes", nullable = false)
  private Integer totalNotes;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
