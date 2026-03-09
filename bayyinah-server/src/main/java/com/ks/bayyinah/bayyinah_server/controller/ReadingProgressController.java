package com.ks.bayyinah.bayyinah_server.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ks.bayyinah.bayyinah_server.dto.ReadingProgressCreationRequest;
import com.ks.bayyinah.bayyinah_server.dto.ReadingProgressDeletionResponse;
import com.ks.bayyinah.bayyinah_server.model.ReadingProgress;
import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.service.ReadingProgressService;

@RestController
@RequestMapping("/api/progress")
public class ReadingProgressController {
  @Autowired
  private ReadingProgressService readingProgressService;

  @GetMapping("")
  public ResponseEntity<List<ReadingProgress>> getReadingProgress() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    List<ReadingProgress> progresses = readingProgressService.getReadingProgressByUserId(currentUser.getId());

    return ResponseEntity.ok(progresses);
  }

  @GetMapping("/chapters/{number}")
  public ResponseEntity<?> getReadingProgressByChapterNumber(@PathVariable("number") Integer number) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    Optional<ReadingProgress> progress = readingProgressService
        .getReadingProgressByUserIdAndSurahNumber(currentUser.getId(), number);

    if (progress.isPresent()) {
      return ResponseEntity.ok(progress.get());
    } else {
      return ResponseEntity.status(404)
          .body(new ReadingProgressDeletionResponse("Reading progress not found for chapter " + number));
    }

  }

  @GetMapping("/current")
  public ResponseEntity<?> getCurrentReadingProgress() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    Optional<ReadingProgress> progress = readingProgressService.getLatestProgress(currentUser.getId());

    if (progress.isPresent()) {
      return ResponseEntity.ok(progress.get());
    } else {
      return ResponseEntity.status(404)
          .body(new ReadingProgressDeletionResponse("No reading progress found for the user"));
    }
  }

  @PostMapping("")
  public ResponseEntity<ReadingProgress> saveReadingProgress(
      @RequestBody ReadingProgressCreationRequest readingProgress) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    ReadingProgress progressToSave = ReadingProgress.builder()
        .userId(currentUser.getId())
        .surahNumber(readingProgress.surahNumber())
        .ayahNumber(readingProgress.ayahNumber())
        .build();
    ReadingProgress savedProgress = readingProgressService.saveReadingProgress(progressToSave);

    return ResponseEntity.ok(savedProgress);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ReadingProgressDeletionResponse> deleteReadingProgressById(@PathVariable("id") Long id) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    readingProgressService.deleteReadingProgressByIdAndUserId(id, currentUser.getId());
    return ResponseEntity.ok(new ReadingProgressDeletionResponse("Reading progress deleted successfully"));
  }
}
