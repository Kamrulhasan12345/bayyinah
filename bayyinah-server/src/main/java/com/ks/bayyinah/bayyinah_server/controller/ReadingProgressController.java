package com.ks.bayyinah.bayyinah_server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

  @GetMapping("/")
  public ResponseEntity<List<ReadingProgress>> getReadingProgress() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    List<ReadingProgress> progresses = readingProgressService.getReadingProgressByUserId(currentUser.getId());

    return ResponseEntity.ok(progresses);
  }

  @GetMapping("/chapters/{number}")
  public ResponseEntity<ReadingProgress> getReadingProgressByChapterNumber(Integer number) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    return readingProgressService.getReadingProgressByUserIdAndSurahNumber(currentUser.getId(), number)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/current")
  public ResponseEntity<ReadingProgress> getCurrentReadingProgress() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    return readingProgressService.getLatestProgress(currentUser.getId())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/")
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
  public ResponseEntity<ReadingProgressDeletionResponse> deleteReadingProgressById(Long id) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    readingProgressService.deleteReadingProgressByIdAndUserId(id, currentUser.getId());
    return ResponseEntity.ok(new ReadingProgressDeletionResponse("Reading progress deleted successfully"));
  }
}
