package com.ks.bayyinah.bayyinah_server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.model.Note;
import com.ks.bayyinah.bayyinah_server.repository.NoteRepository;

import jakarta.transaction.Transactional;

@Service
public class NoteService {

  @Autowired
  private NoteRepository noteRepository;

  public List<Note> getNotesByUserId(Long userId) {
    return noteRepository.findByUserIdOrderByUpdatedAtDesc(userId);
  }

  public Optional<Note> getNoteByIdAndUserId(Long id, Long userId) {
    return noteRepository.findByIdAndUserId(id, userId);
  }

  public List<Note> getNotesBySurahNumber(Long userId, Integer surahNumber) {
    return noteRepository.findByUserIdAndSurahNumber(userId, surahNumber);
  }

  public List<Note> getNotesBySurahAndAyah(Long userId, Integer surahNumber, Integer ayahNumber) {
    return noteRepository.findByUserIdAndSurahNumberAndAyahNumber(userId, surahNumber, ayahNumber);
  }

  public Note saveNote(Note note) {
    return noteRepository.save(note);
  }

  @Transactional
  public Optional<Note> updateNote(Long id, Long userId, String content, Boolean isPrivate) {
    Optional<Note> existing = noteRepository.findByIdAndUserId(id, userId);
    if (existing.isEmpty()) {
      return Optional.empty();
    }
    Note note = existing.get();
    if (content != null && !content.isBlank()) {
      note.setContent(content);
    }
    if (isPrivate != null) {
      note.setIsPrivate(isPrivate);
    }
    return Optional.of(noteRepository.save(note));
  }

  @Transactional
  public void deleteNoteByIdAndUserId(Long id, Long userId) {
    noteRepository.deleteByUserIdAndId(userId, id);
  }

  public List<Note> searchNotes(Long userId, String query) {
    return noteRepository.findByUserIdAndContentContainingIgnoreCase(userId, query);
  }

  public List<Note> getNotesSince(Long userId, LocalDateTime since) {
    return noteRepository.findByUserIdAndUpdatedAtAfter(userId, since);
  }

  public long countNotesByUserId(Long userId) {
    return noteRepository.countByUserId(userId);
  }
}
