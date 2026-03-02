package com.ks.bayyinah.bayyinah_server.repository;

import com.ks.bayyinah.bayyinah_server.model.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

  List<ReadingProgress> findByUserIdOrderByLastReadAtDesc(Long userId);

  Optional<ReadingProgress> findFirstByUserIdOrderByLastReadAtDesc(Long userId);

  List<ReadingProgress> findByUserIdAndSurahNumber(Long userId, Integer surahNumber);

  Optional<ReadingProgress> findByUserIdAndSurahNumberAndAyahNumber(
      Long userId,
      Integer surahNumber,
      Integer ayahNumber);

  void deleteByUserId(Long userId);

  // For sync
  List<ReadingProgress> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime timestamp);

  // Statistics
  @Query("SELECT COUNT(DISTINCT rp.surahNumber) FROM ReadingProgress rp " +
      "WHERE rp.userId = :userId AND rp.completionPercentage = 100")
  long countCompletedSurahs(Long userId);

  @Query("SELECT AVG(rp.completionPercentage) FROM ReadingProgress rp " +
      "WHERE rp.userId = :userId")
  Double getAverageCompletion(Long userId);
}
