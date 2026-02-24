package com.ks.bayyinah.bayyinah_server.repository;

import com.ks.bayyinah.bayyinah_server.model.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {

  List<RecommendationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<RecommendationHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

  void deleteByUserId(Long userId);

  long countByUserId(Long userId);

  // Analytics - most queried emotions/keywords
  @Query("SELECT rh.queryText, COUNT(rh) as count FROM RecommendationHistory rh " +
      "WHERE rh.userId = :userId " +
      "GROUP BY rh.queryText " +
      "ORDER BY count DESC")
  List<Object[]> findTopQueriesByUserId(Long userId);
}
