package com.ks.bayyinah.bayyinah_server.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "reading_progresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingProgress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "surah_number", nullable = false)
  private Integer surahNumber;

  @Column(name = "ayah_number", nullable = false)
  private Integer ayahNumber;

  @Column(name = "last_read_at", nullable = false)
  private LocalDateTime lastReadAt;

  @Column(name = "completion_percentage", nullable = false)
  private Float completionPercentage;

  @Column(name = "total_read_time_minutes", nullable = false)
  private Integer totalReadTimeMinutes;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "user_id", nullable = false)
  private Long userId;
}
