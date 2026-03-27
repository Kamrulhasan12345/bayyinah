package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.hybrid.model.Bookmark;
import com.ks.bayyinah.infra.hybrid.model.ReadingProgress;
import com.ks.bayyinah.infra.hybrid.model.SyncQueueItem;
import com.ks.bayyinah.infra.remote.client.ApiClient.ApiException;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteBookmarkDto;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteBookmarkUpsertRequest;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteReadingProgressDto;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteReadingProgressUpsertRequest;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteUserPreferenceDto;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteUserPreferenceResponse;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteUserPreferenceUpdateRequest;
import com.ks.bayyinah.infra.remote.query.RemoteSyncQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import tools.jackson.databind.ObjectMapper;

public class SyncOrchestratorService {
  private static final int MAX_QUEUE_BATCH = 50;
  private static final Logger logger = LoggerFactory.getLogger(SyncOrchestratorService.class);

  private final SyncQueueService syncQueueService;
  private final BookmarkService bookmarkService;
  private final ReadingProgressService readingProgressService;
  private final UserPreferenceService userPreferenceService;
  private final UserService userService;
  private final RemoteSyncQueryService remoteSyncQueryService;
  private final ObjectMapper objectMapper;

  public SyncOrchestratorService(
      SyncQueueService syncQueueService,
      BookmarkService bookmarkService,
      ReadingProgressService readingProgressService,
      UserPreferenceService userPreferenceService,
      UserService userService,
      RemoteSyncQueryService remoteSyncQueryService) {
    this(
        syncQueueService,
        bookmarkService,
        readingProgressService,
        userPreferenceService,
        userService,
        remoteSyncQueryService,
        null);
  }

  public SyncOrchestratorService(
      SyncQueueService syncQueueService,
      BookmarkService bookmarkService,
      ReadingProgressService readingProgressService,
      UserPreferenceService userPreferenceService,
      UserService userService,
      RemoteSyncQueryService remoteSyncQueryService,
      ObjectMapper objectMapper) {
    this.syncQueueService = syncQueueService;
    this.bookmarkService = bookmarkService;
    this.readingProgressService = readingProgressService;
    this.userPreferenceService = userPreferenceService;
    this.userService = userService;
    this.remoteSyncQueryService = remoteSyncQueryService;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
  }

  public CompletableFuture<Void> runSyncNowAsync() {
    return CompletableFuture.runAsync(this::runSyncNow);
  }

  public void runSyncNow() {
    logger.info("Sync run started");
    pushPendingQueue();
    pullLatestState();
    userService.updateLastSyncAt(LocalDateTime.now());
    logger.info("Sync run completed successfully");
  }

  private void pushPendingQueue() {
    List<SyncQueueItem> pending = syncQueueService.getPending(MAX_QUEUE_BATCH);
    logger.info("Sync push phase started. pendingItems={}", pending.size());
    for (SyncQueueItem item : pending) {
      if (item.getId() == null) {
        logger.warn("Skipping queue item with null id. table={} operation={}", item.getTableName(), item.getOperation());
        continue;
      }

      logger.info("Processing queue item. id={} table={} operation={} recordId={}",
          item.getId(), item.getTableName(), item.getOperation(), item.getRecordId());

      try {
        processQueueItem(item);
        syncQueueService.markCompleted(item.getId());
        logger.info("Queue item completed. id={}", item.getId());
      } catch (ApiException e) {
        if (isNonRetryableApiError(e)) {
          syncQueueService.markCompleted(item.getId());
          logger.warn("Queue item completed as non-retryable API failure. id={} error={}",
              item.getId(), safeError(e));
        } else {
          syncQueueService.markFailed(item.getId(), safeError(e));
          logger.warn("Queue item marked failed (retryable API failure). id={} error={}",
              item.getId(), safeError(e));
        }
      } catch (Exception e) {
        syncQueueService.markFailed(item.getId(), safeError(e));
        logger.error("Queue item marked failed (unexpected exception). id={} error={}",
            item.getId(), safeError(e), e);
      }
    }
    logger.info("Sync push phase finished");
  }

  private void processQueueItem(SyncQueueItem item) throws Exception {
    String table = item.getTableName() == null ? "" : item.getTableName().trim().toLowerCase();
    String operation = item.getOperation() == null ? "" : item.getOperation().trim().toUpperCase();

    switch (table) {
      case "bookmarks" -> processBookmarkItem(item, operation);
      case "reading_progress" -> processReadingProgressItem(item, operation);
      case "user_preferences" -> processUserPreferenceItem(operation);
      default -> {
        logger.warn("Unknown queue item table. itemId={} table={} operation={}",
            item.getId(), item.getTableName(), item.getOperation());
      }
    }
  }

  private void processBookmarkItem(SyncQueueItem item, String operation) throws Exception {
    if ("DELETE".equals(operation)) {
      Optional<Bookmark> bookmark = bookmarkService.getById(item.getRecordId());
      if (bookmark.isPresent() && bookmark.get().getServerId() != null) {
        logger.info("Pushing bookmark delete. queueId={} localRecordId={} serverId={}",
            item.getId(), item.getRecordId(), bookmark.get().getServerId());
        await(remoteSyncQueryService.deleteBookmark(bookmark.get().getServerId()));
      } else {
        logger.info("Skipping bookmark delete push due to missing server mapping. queueId={} localRecordId={}",
            item.getId(), item.getRecordId());
      }
      return;
    }

    BookmarkPayload payload = objectMapper.readValue(item.getPayload(), BookmarkPayload.class);
    RemoteBookmarkUpsertRequest request = new RemoteBookmarkUpsertRequest(
        payload.surahNumber(), payload.ayahNumber(), payload.title(), payload.color());

    try {
      logger.info("Pushing bookmark upsert. queueId={} localRecordId={} surah={} ayah={}",
          item.getId(), item.getRecordId(), payload.surahNumber(), payload.ayahNumber());
        RemoteBookmarkDto created = await(remoteSyncQueryService.createBookmark(request));
      if (item.getRecordId() != null && created != null && created.id() != null) {
        bookmarkService.markAsSynced(item.getRecordId(), created.id());
        logger.info("Bookmark upsert acknowledged. queueId={} localRecordId={} serverId={}",
            item.getId(), item.getRecordId(), created.id());
      }
    } catch (ApiException e) {
      if (!isDuplicateBookmarkError(e)) {
        throw e;
      }

      logger.warn("Bookmark duplicate detected during push. queueId={} surah={} ayah={}. Attempting reconciliation.",
          item.getId(), payload.surahNumber(), payload.ayahNumber());

      RemoteBookmarkDto existingRemote = findRemoteBookmarkByVerse(payload.surahNumber(), payload.ayahNumber());
      if (item.getRecordId() != null && existingRemote != null && existingRemote.id() != null) {
        bookmarkService.markAsSynced(item.getRecordId(), existingRemote.id());
        logger.info("Duplicate bookmark reconciled. queueId={} localRecordId={} serverId={}",
            item.getId(), item.getRecordId(), existingRemote.id());
      } else {
        logger.warn("Duplicate bookmark reconciliation could not find remote record. queueId={}", item.getId());
      }
    }
  }

  private void processReadingProgressItem(SyncQueueItem item, String operation) throws Exception {
    if ("DELETE".equals(operation)) {
      Optional<ReadingProgress> progress = readingProgressService.getById(item.getRecordId());
      if (progress.isPresent() && progress.get().getServerId() != null) {
        logger.info("Pushing reading progress delete. queueId={} localRecordId={} serverId={}",
            item.getId(), item.getRecordId(), progress.get().getServerId());
        await(remoteSyncQueryService.deleteReadingProgress(progress.get().getServerId()));
      } else {
        logger.info("Skipping reading progress delete push due to missing server mapping. queueId={} localRecordId={}",
            item.getId(), item.getRecordId());
      }
      return;
    }

    ReadingProgressPayload payload = objectMapper.readValue(item.getPayload(), ReadingProgressPayload.class);
    RemoteReadingProgressUpsertRequest request = new RemoteReadingProgressUpsertRequest(
        payload.surahNumber(), payload.ayahNumber());

    logger.info("Pushing reading progress upsert. queueId={} localRecordId={} surah={} ayah={}",
        item.getId(), item.getRecordId(), payload.surahNumber(), payload.ayahNumber());
    RemoteReadingProgressDto created = await(remoteSyncQueryService.upsertReadingProgress(request));
    if (item.getRecordId() != null && created != null && created.id() != null) {
      readingProgressService.markAsSynced(item.getRecordId(), created.id());
      logger.info("Reading progress upsert acknowledged. queueId={} localRecordId={} serverId={}",
          item.getId(), item.getRecordId(), created.id());
    }
  }

  private void processUserPreferenceItem(String operation) {
    if (!"UPSERT".equals(operation)) {
      return;
    }

    Map<String, String> preferences = userPreferenceService.getAllAsMap();
    logger.info("Pushing user preferences snapshot. keyCount={}", preferences.size());
    RemoteUserPreferenceUpdateRequest request = buildPreferenceRequest(preferences);
    await(remoteSyncQueryService.updateUserPreferences(request));
    logger.info("User preferences push acknowledged");
  }

  private void pullLatestState() {
    logger.info("Sync pull phase started");
    RemoteBookmarkDto[] remoteBookmarks = await(remoteSyncQueryService.getBookmarks());
    logger.info("Pulled bookmarks from server. count={}", remoteBookmarks == null ? 0 : remoteBookmarks.length);
    if (remoteBookmarks != null) {
      for (RemoteBookmarkDto bookmark : remoteBookmarks) {
        if (bookmark == null || bookmark.id() == null || bookmark.surahNumber() == null || bookmark.ayahNumber() == null) {
          continue;
        }
        bookmarkService.upsertFromRemote(
            bookmark.id(),
            bookmark.surahNumber(),
            bookmark.ayahNumber(),
            bookmark.title(),
            bookmark.color());
      }
    }

    RemoteReadingProgressDto[] remoteProgress = await(remoteSyncQueryService.getReadingProgress());
    logger.info("Pulled reading progress from server. count={}", remoteProgress == null ? 0 : remoteProgress.length);
    if (remoteProgress != null) {
      for (RemoteReadingProgressDto progress : remoteProgress) {
        if (progress == null || progress.id() == null || progress.surahNumber() == null || progress.ayahNumber() == null) {
          continue;
        }
        readingProgressService.upsertFromRemote(
            progress.id(),
            progress.surahNumber(),
            progress.ayahNumber(),
            progress.lastReadAt(),
            progress.totalReadTimeMinutes());
      }
    }

    RemoteUserPreferenceResponse remotePreferenceResponse = await(remoteSyncQueryService.getUserPreferences());
    if (remotePreferenceResponse != null && remotePreferenceResponse.userPreference() != null) {
      userPreferenceService.applyRemotePreferences(toPreferenceMap(remotePreferenceResponse.userPreference()));
      logger.info("Pulled and applied user preferences from server");
    } else {
      logger.info("No remote user preferences returned from server");
    }
    logger.info("Sync pull phase finished");
  }

  private Map<String, String> toPreferenceMap(RemoteUserPreferenceDto dto) {
    return Map.of(
        "theme", defaultString(dto.theme(), "light"),
        "font_size", String.valueOf(dto.fontSize() == null ? 16 : dto.fontSize()),
        "default_translation", String.valueOf(dto.defaultTranslation() == null ? 20 : dto.defaultTranslation()),
        "language", defaultString(dto.language(), "en"),
        "reading_mode", defaultString(dto.readingMode(), "continuous"),
        "show_transliteration", String.valueOf(Boolean.TRUE.equals(dto.showTransliteration())),
        "auto_scroll", String.valueOf(dto.autoScroll() == null || dto.autoScroll()));
  }

  private RemoteUserPreferenceUpdateRequest buildPreferenceRequest(Map<String, String> prefs) {
    return new RemoteUserPreferenceUpdateRequest(
        prefs.getOrDefault("theme", "light"),
        parseInt(prefs.get("font_size"), 16),
        parseInt(prefs.get("default_translation"), 20),
        prefs.getOrDefault("language", "en"),
        prefs.getOrDefault("reading_mode", "continuous"),
        parseBoolean(prefs.get("show_transliteration"), false),
        parseBoolean(prefs.get("auto_scroll"), true));
  }

  private int parseInt(String value, int fallback) {
    try {
      return Integer.parseInt(value == null ? "" : value.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private boolean parseBoolean(String value, boolean fallback) {
    if (value == null) {
      return fallback;
    }
    return "true".equalsIgnoreCase(value.trim());
  }

  private boolean isNonRetryableApiError(ApiException exception) {
    String message = exception.getMessage();
    if (message == null) {
      return false;
    }

    String normalized = message.toLowerCase();

    return message.contains("status: 400")
        || message.contains("status: 403")
        || message.contains("status: 404")
        || message.contains("Unauthorized")
        || normalized.contains("already exists")
        || normalized.contains("invalid")
        || normalized.contains("forbidden")
        || normalized.contains("not found");
  }

  private boolean isDuplicateBookmarkError(ApiException exception) {
    String message = exception.getMessage();
    if (message == null) {
      return false;
    }

    String normalized = message.toLowerCase();
    return normalized.contains("bookmark already exists") || normalized.contains("already exists for this verse");
  }

  private RemoteBookmarkDto findRemoteBookmarkByVerse(Integer surahNumber, Integer ayahNumber) {
    if (surahNumber == null || ayahNumber == null) {
      return null;
    }

    RemoteBookmarkDto[] remoteBookmarks = await(remoteSyncQueryService.getBookmarks());
    if (remoteBookmarks == null) {
      return null;
    }

    for (RemoteBookmarkDto remoteBookmark : remoteBookmarks) {
      if (remoteBookmark == null) {
        continue;
      }
      if (surahNumber.equals(remoteBookmark.surahNumber()) && ayahNumber.equals(remoteBookmark.ayahNumber())) {
        return remoteBookmark;
      }
    }
    return null;
  }

  private String safeError(Throwable throwable) {
    Throwable unwrapped = unwrapThrowable(throwable);
    String message = unwrapped.getMessage();
    if (message == null || message.isBlank()) {
      return unwrapped.getClass().getSimpleName();
    }
    return message;
  }

  private <T> T await(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException e) {
      throw rethrowUnwrapped(e);
    }
  }

  private RuntimeException rethrowUnwrapped(Throwable throwable) {
    Throwable unwrapped = unwrapThrowable(throwable);
    if (unwrapped instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    return new RuntimeException(unwrapped.getMessage(), unwrapped);
  }

  private Throwable unwrapThrowable(Throwable throwable) {
    Throwable current = throwable;
    while (current instanceof CompletionException
        || current instanceof ExecutionException
        || current instanceof InvocationTargetException) {
      Throwable cause = current.getCause();
      if (cause == null || cause == current) {
        break;
      }
      current = cause;
    }
    return current;
  }

  private String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private record BookmarkPayload(
      Integer surahNumber,
      Integer ayahNumber,
      String title,
      String color) {
  }

  private record ReadingProgressPayload(
      Integer surahNumber,
      Integer ayahNumber,
      String lastReadAt,
      Integer timeSpentSeconds) {
  }
}
