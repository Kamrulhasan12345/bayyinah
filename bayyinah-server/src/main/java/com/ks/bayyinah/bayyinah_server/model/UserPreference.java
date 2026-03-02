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
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "theme", nullable = false)
  private String theme;

  @Column(name = "font_size", nullable = false)
  private Integer fontSize;

  @Column(name = "default_translation", nullable = false)
  private Integer defaultTranslation;

  @Column(name = "language", nullable = false)
  private String language;

  @Column(name = "reading_mode", nullable = false)
  private String readingMode;

  @Column(name = "show_transliteration", nullable = false)
  private Boolean showTransliteration;

  @Column(name = "auto_scroll", nullable = false)
  private Boolean autoScroll;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;
}
