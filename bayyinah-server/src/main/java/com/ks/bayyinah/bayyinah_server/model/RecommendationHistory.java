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
@Table(name = "recommendation_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
  private String queryText;
  // private RecommendedAyah recommendedAyahs;

  @Column(name = "feedback_rating", nullable = false)
  private Integer feedbackRating;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
