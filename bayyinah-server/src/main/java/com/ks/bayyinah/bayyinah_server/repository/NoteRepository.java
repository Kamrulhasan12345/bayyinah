package com.ks.bayyinah.bayyinah_server.repository;

import com.ks.bayyinah.bayyinah_server.model.Note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

  List<Note> findByUserIdOrderByUpdatedAtDesc(Long userId);

  List<Note> findByUserIdAndSurahNumber(Long userId, Integer surahNumber);

  List<Note> findByUserIdAndSurahNumberAndAyahNumber(
      Long userId,
      Integer surahNumber,
      Integer ayahNumber);

  Optional<Note> findByIdAndUserId(Long id, Long userId);

  void deleteByUserIdAndId(Long userId, Long noteId);

  void deleteByUserId(Long userId);


  List<Note> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime timestamp);
  List<Note> findByUserIdAndContentContainingIgnoreCase(Long userId, String query);

  long countByUserId(Long userId);

}