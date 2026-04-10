package com.ks.bayyinah.bayyinah_server.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ks.bayyinah.bayyinah_server.dto.NoteCreationRequest;
import com.ks.bayyinah.bayyinah_server.dto.NoteDeleteResponse;
import com.ks.bayyinah.bayyinah_server.dto.NoteUpdateRequest;
import com.ks.bayyinah.bayyinah_server.model.Note;
import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.service.NoteService;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

  @Autowired
  private NoteService noteService;

  @GetMapping("")
  public ResponseEntity<List<Note>> getNotes() {
    User currentUser = getCurrentUser();
    List<Note> notes = noteService.getNotesByUserId(currentUser.getId());
    return ResponseEntity.ok(notes);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getNoteById(@PathVariable("id") Long id) {
    User currentUser = getCurrentUser();
    Optional<Note> note = noteService.getNoteByIdAndUserId(id, currentUser.getId());

    if (note.isPresent()) {
      return ResponseEntity.ok(note.get());
    } else {
      return ResponseEntity.status(404).body(new NoteDeleteResponse("Note not found"));
    }
  }

  @GetMapping("/chapters/{number}")
  public ResponseEntity<List<Note>> getNotesByChapterNumber(@PathVariable("number") int surahNumber) {
    User currentUser = getCurrentUser();
    List<Note> notes = noteService.getNotesBySurahNumber(currentUser.getId(), surahNumber);
    return ResponseEntity.ok(notes);
  }

  @GetMapping("/chapters/{number}/ayahs/{ayah}")
  public ResponseEntity<List<Note>> getNotesByAyah(
      @PathVariable("number") int surahNumber,
      @PathVariable("ayah") int ayahNumber) {
    User currentUser = getCurrentUser();
    List<Note> notes = noteService.getNotesBySurahAndAyah(currentUser.getId(), surahNumber, ayahNumber);
    return ResponseEntity.ok(notes);
  }

  @GetMapping("/search")
  public ResponseEntity<List<Note>> searchNotes(@RequestParam("q") String query) {
    User currentUser = getCurrentUser();
    List<Note> notes = noteService.searchNotes(currentUser.getId(), query);
    return ResponseEntity.ok(notes);
  }

  @GetMapping("/sync")
  public ResponseEntity<List<Note>> syncNotes(
      @RequestParam("since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
    User currentUser = getCurrentUser();
    List<Note> notes = noteService.getNotesSince(currentUser.getId(), since);
    return ResponseEntity.ok(notes);
  }

  @PostMapping("")
  public ResponseEntity<?> createNote(@RequestBody NoteCreationRequest request) {
    User currentUser = getCurrentUser();

    Note newNote = Note.builder()
        .userId(currentUser.getId())
        .surahNumber(request.surahNumber())
        .ayahNumber(request.ayahNumber())
        .content(request.content())
        .isPrivate(request.isPrivate() != null ? request.isPrivate() : true)
        .build();

    Note createdNote = noteService.saveNote(newNote);
    return ResponseEntity.ok(createdNote);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> updateNote(
      @PathVariable("id") Long id,
      @RequestBody NoteUpdateRequest request) {
    User currentUser = getCurrentUser();

    Optional<Note> updated = noteService.updateNote(
        id, currentUser.getId(), request.content(), request.isPrivate());

    if (updated.isPresent()) {
      return ResponseEntity.ok(updated.get());
    } else {
      return ResponseEntity.status(404).body(new NoteDeleteResponse("Note not found"));
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<NoteDeleteResponse> deleteNoteById(@PathVariable("id") Long id) {
    User currentUser = getCurrentUser();
    noteService.deleteNoteByIdAndUserId(id, currentUser.getId());
    return ResponseEntity.ok(new NoteDeleteResponse("Note deleted successfully"));
  }

  private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    return userDetails.getUser();
  }
}