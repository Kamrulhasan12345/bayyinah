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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/progress")
public class ReadingProgressController {
  private static final Logger logger = LoggerFactory.getLogger(ReadingProgressController.class);

  @Autowired
  private ReadingProgressService readingProgressService;

  @GetMapping("")
  public ResponseEntity<List<ReadingProgress>> getReadingProgress() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    List<ReadingProgress> progresses = readingProgressService.getReadingProgressByUserId(currentUser.getId());
    logger.info("Reading progress fetched. userId={} count={}", currentUser.getId(), progresses.size());

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

    logger.info("Reading progress create requested. userId={} surah={} ayah={}",
      currentUser.getId(), readingProgress.surahNumber(), readingProgress.ayahNumber());

    ReadingProgress progressToSave = ReadingProgress.builder()
        .userId(currentUser.getId())
        .surahNumber(readingProgress.surahNumber())
        .ayahNumber(readingProgress.ayahNumber())
        .build();
    ReadingProgress savedProgress = readingProgressService.saveReadingProgress(progressToSave);
    logger.info("Reading progress created. userId={} progressId={} surah={} ayah={}",
      currentUser.getId(), savedProgress.getId(), savedProgress.getSurahNumber(), savedProgress.getAyahNumber());

    return ResponseEntity.ok(savedProgress);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ReadingProgressDeletionResponse> deleteReadingProgressById(@PathVariable("id") Long id) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    logger.info("Reading progress delete requested. userId={} progressId={}", currentUser.getId(), id);

    readingProgressService.deleteReadingProgressByIdAndUserId(id, currentUser.getId());
    logger.info("Reading progress delete completed. userId={} progressId={}", currentUser.getId(), id);
    return ResponseEntity.ok(new ReadingProgressDeletionResponse("Reading progress deleted successfully"));
  }
}
