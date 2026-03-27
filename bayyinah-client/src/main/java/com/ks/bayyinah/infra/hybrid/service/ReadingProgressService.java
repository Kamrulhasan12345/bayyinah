package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.hybrid.model.ReadingProgress;
import com.ks.bayyinah.infra.local.repository.user.ReadingProgressRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;

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
  private final ObjectMapper objectMapper;

  public ReadingProgressService(ReadingProgressRepository repository) {
    this(repository, null, null);
  }

  public ReadingProgressService(ReadingProgressRepository repository, SyncQueueService syncQueueService) {
    this(repository, syncQueueService, null);
  }

  public ReadingProgressService(
      ReadingProgressRepository repository,
      SyncQueueService syncQueueService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.syncQueueService = syncQueueService;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
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

  public Optional<ReadingProgress> getById(Long id) {
    return repository.findById(id);
  }

  public Optional<ReadingProgress> getByServerId(Long serverId) {
    if (serverId == null) {
      return Optional.empty();
    }
    return repository.findByServerId(serverId);
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

  public void upsertFromRemote(Long serverId, int surahNumber, int ayahNumber, LocalDateTime lastReadAt,
      Integer totalReadTimeMinutes) {
    Optional<ReadingProgress> existingByServerId = repository.findByServerId(serverId);
    Optional<ReadingProgress> existingByVerse = repository.findByVerse(surahNumber, ayahNumber);

    ReadingProgress target;
    if (existingByServerId.isPresent()) {
      target = existingByServerId.get();
    } else if (existingByVerse.isPresent()) {
      target = existingByVerse.get();
    } else {
      target = new ReadingProgress(surahNumber, ayahNumber);
    }

    target.setSurahNumber(surahNumber);
    target.setAyahNumber(ayahNumber);
    target.setLastReadAt(lastReadAt != null ? lastReadAt : LocalDateTime.now());
    target.setTimeSpentSeconds(Math.max(0, (totalReadTimeMinutes == null ? 0 : totalReadTimeMinutes) * 60));
    target.setServerId(serverId);
    target.setSynced(true);
    target.setDeleted(false);

    if (target.getId() == null) {
      repository.insert(target);
    } else {
      repository.update(target);
    }
  }

  private void enqueueProgressUpsert(ReadingProgress progress) {
    if (syncQueueService == null || progress.getId() == null) {
      return;
    }

    String payload = objectMapper.writeValueAsString(new ReadingProgressPayload(
        progress.getSurahNumber(),
        progress.getAyahNumber(),
        progress.getLastReadAt() == null ? null : progress.getLastReadAt().toString(),
        progress.getTimeSpentSeconds()));
    syncQueueService.enqueueUpsert("reading_progress", progress.getId(), payload);
  }

  private record ReadingProgressPayload(
      Integer surahNumber,
      Integer ayahNumber,
      String lastReadAt,
      Integer timeSpentSeconds) {
  }

}
