package com.ks.bayyinah.bayyinah_server.repository;

import com.ks.bayyinah.bayyinah_server.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

  List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<Bookmark> findByUserIdAndSurahNumber(Long userId, Integer surahNumber);

  Optional<Bookmark> findByUserIdAndSurahNumberAndAyahNumber(
      Long userId,
      Integer surahNumber,
      Integer ayahNumber);

  boolean existsByUserIdAndSurahNumberAndAyahNumber(
      Long userId,
      Integer surahNumber,
      Integer ayahNumber);

  void deleteByUserIdAndId(Long userId, Long bookmarkId);

  void deleteByUserId(Long userId);

  // For sync - get bookmarks modified after timestamp
  List<Bookmark> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime timestamp);

  long countByUserId(Long userId);
}
