package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.hybrid.model.ReadingProgress;
import com.ks.bayyinah.infra.local.repository.user.ReadingProgressRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReadingProgressService {
  // This service will manage the reading progress of the user. It will interact
  // with the local database to save and retrieve the reading progress, and also
  // sync with the server when needed.

  // For simplicity, let's assume the reading progress is represented by a
  // chapter number and a verse number.

  // We will need methods to:
  // - Update reading progress
  // - Get current reading progress
  // - Sync reading progress with the server

  // The implementation details will depend on the specific requirements and data
  // models of the application.

  private final ReadingProgressRepository repository;
  private final SyncQueueService syncQueueService;

  public ReadingProgressService(ReadingProgressRepository repository) {
    this(repository, null);
  }

  public ReadingProgressService(ReadingProgressRepository repository, SyncQueueService syncQueueService) {
    this.repository = repository;
    this.syncQueueService = syncQueueService;
  }

  public void recordProgress(int surahNumber, int ayahNumber) {
    recordProgress(surahNumber, ayahNumber, 0);
  }

  public void recordProgress(int surahNumber, int ayahNumber, int deltaTimeSeconds) {
    Optional<ReadingProgress> existing = repository.findByVerse(surahNumber, ayahNumber);
    if (existing.isPresent()) {
      ReadingProgress progress = existing.get();
      progress.setSurahNumber(surahNumber);
      progress.setAyahNumber(ayahNumber);
      progress.setLastReadAt(LocalDateTime.now());
      if (deltaTimeSeconds > 0) {
        progress.setTimeSpentSeconds(progress.getTimeSpentSeconds() + deltaTimeSeconds);
      }
      progress.setSynced(false);
      progress.setDeleted(false);
      repository.update(progress);
      enqueueProgressUpsert(progress);
      return;
    }

    ReadingProgress progress = new ReadingProgress(surahNumber, ayahNumber);
    if (deltaTimeSeconds > 0) {
      progress.setTimeSpentSeconds(deltaTimeSeconds);
    }
    progress.setLastReadAt(LocalDateTime.now());
    progress.setSynced(false);
    progress.setDeleted(false);
    repository.insert(progress);
    enqueueProgressUpsert(progress);
  }

  public Optional<ReadingProgress> getLatestProgress() {
    return repository.findLatest();
  }

  public Optional<ReadingProgress> getProgressByVerse(int surahNumber, int ayahNumber) {
    return repository.findByVerse(surahNumber, ayahNumber);
  }

  public ReadingProgress getLatestProgressForSurah(int surahNumber) {
    return repository.findBySurah(surahNumber);
  }

  public List<ReadingProgress> getAllProgress() {
    return repository.findAll();
  }

  public List<ReadingProgress> getUnsyncedProgress() {
    return repository.findUnsynced();
  }

  public void markAsSynced(Long id, Long serverId) {
    repository.markAsSynced(id, serverId);
  }

  private void enqueueProgressUpsert(ReadingProgress progress) {
    if (syncQueueService == null || progress.getId() == null) {
      return;
    }

    String payload = "{" +
        "\"surahNumber\":" + progress.getSurahNumber() + "," +
        "\"ayahNumber\":" + progress.getAyahNumber() + "," +
        "\"lastReadAt\":\"" + progress.getLastReadAt() + "\"," +
        "\"timeSpentSeconds\":" + progress.getTimeSpentSeconds() +
        "}";
    syncQueueService.enqueueUpsert("reading_progress", progress.getId(), payload);
  }

}
