package com.ks.bayyinah.bayyinah_server.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "bookmarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bookmark {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "surah_number", nullable = false)
  private Integer surahNumber;

  @Column(name = "ayah_number", nullable = false)
  private Integer ayahNumber;

  @Column(name = "title")
  private String title;

  @Column(name = "color")
  private String color;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();

    if (this.title == null || this.title.isEmpty()) {
      this.title = "Bookmark for Surah " + this.surahNumber + ", Ayah " + this.ayahNumber;
    }

    if (this.color == null || this.color.isEmpty()) {
      this.color = "#00FF00";
    }
  }
}
