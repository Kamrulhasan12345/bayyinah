package com.ks.bayyinah.bayyinah_server.repository;

import com.ks.bayyinah.bayyinah_server.model.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

  List<ReadingProgress> findByUserId(Long userId);

  List<ReadingProgress> findByUserIdOrderByLastReadAtDesc(Long userId);

  Optional<ReadingProgress> findFirstByUserIdOrderByLastReadAtDesc(Long userId);

  Optional<ReadingProgress> findByUserIdAndSurahNumber(Long userId, Integer surahNumber);

  void deleteByUserId(Long userId);

  void deleteByUserIdAndSurahNumber(Long userId, Integer surahNumber);

  void deleteByUserIdAndId(Long userId, Long id);

  // For sync
  List<ReadingProgress> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime timestamp);

  // Statistics
  @Query("SELECT COUNT(DISTINCT rp.surahNumber) FROM ReadingProgress rp " +
      "WHERE rp.userId = :userId AND rp.completionPercentage = 100")
  long countCompletedSurahs(@Param("userId") Long userId);

  @Query("SELECT AVG(rp.completionPercentage) FROM ReadingProgress rp " +
      "WHERE rp.userId = :userId")
  Double getAverageCompletion(@Param("userId") Long userId);
}
