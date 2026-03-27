package com.ks.bayyinah.infra.remote.query;

import com.ks.bayyinah.infra.remote.client.ApiClient;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteBookmarkDto;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteBookmarkUpsertRequest;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteReadingProgressDto;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteReadingProgressUpsertRequest;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteUserPreferenceResponse;
import com.ks.bayyinah.infra.remote.dto.sync.RemoteUserPreferenceUpdateRequest;
import com.ks.bayyinah.infra.remote.routing.ApiRoute;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class RemoteSyncQueryService {
  private static final Logger logger = LoggerFactory.getLogger(RemoteSyncQueryService.class);
  private final ApiClient apiClient;

  public CompletableFuture<RemoteBookmarkDto[]> getBookmarks() {
    logger.debug("HTTP GET {}", ApiRoute.BOOKMARKS.getPath());
    return apiClient.get(ApiRoute.BOOKMARKS, RemoteBookmarkDto[].class)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP GET {} failed: {}", ApiRoute.BOOKMARKS.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP GET {} success (count={})", ApiRoute.BOOKMARKS.getPath(), result == null ? 0 : result.length);
        });
  }

  public CompletableFuture<RemoteBookmarkDto> createBookmark(RemoteBookmarkUpsertRequest request) {
    logger.debug("HTTP POST {} payload surah={} ayah={}", ApiRoute.BOOKMARKS.getPath(), request.surahNumber(),
        request.ayahNumber());
    return apiClient.post(ApiRoute.BOOKMARKS, request, RemoteBookmarkDto.class)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP POST {} failed: {}", ApiRoute.BOOKMARKS.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP POST {} success serverId={}", ApiRoute.BOOKMARKS.getPath(), result == null ? null : result.id());
        });
  }

  public CompletableFuture<Void> deleteBookmark(Long id) {
    logger.debug("HTTP DELETE {} id={}", ApiRoute.BOOKMARKS_BY_ID.getPath(), id);
    return apiClient.delete(ApiRoute.BOOKMARKS_BY_ID, Void.class, id)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP DELETE {} failed: {}", ApiRoute.BOOKMARKS_BY_ID.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP DELETE {} success id={}", ApiRoute.BOOKMARKS_BY_ID.getPath(), id);
        });
  }

  public CompletableFuture<RemoteReadingProgressDto[]> getReadingProgress() {
    logger.debug("HTTP GET {}", ApiRoute.PROGRESS.getPath());
    return apiClient.get(ApiRoute.PROGRESS, RemoteReadingProgressDto[].class)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP GET {} failed: {}", ApiRoute.PROGRESS.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP GET {} success (count={})", ApiRoute.PROGRESS.getPath(), result == null ? 0 : result.length);
        });
  }

  public CompletableFuture<RemoteReadingProgressDto> upsertReadingProgress(RemoteReadingProgressUpsertRequest request) {
    logger.debug("HTTP POST {} payload surah={} ayah={}", ApiRoute.PROGRESS.getPath(), request.surahNumber(),
        request.ayahNumber());
    return apiClient.post(ApiRoute.PROGRESS, request, RemoteReadingProgressDto.class)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP POST {} failed: {}", ApiRoute.PROGRESS.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP POST {} success serverId={}", ApiRoute.PROGRESS.getPath(), result == null ? null : result.id());
        });
  }

  public CompletableFuture<Void> deleteReadingProgress(Long id) {
    logger.debug("HTTP DELETE {} id={}", ApiRoute.PROGRESS_BY_ID.getPath(), id);
    return apiClient.delete(ApiRoute.PROGRESS_BY_ID, Void.class, id)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP DELETE {} failed: {}", ApiRoute.PROGRESS_BY_ID.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP DELETE {} success id={}", ApiRoute.PROGRESS_BY_ID.getPath(), id);
        });
  }

  public CompletableFuture<RemoteUserPreferenceResponse> getUserPreferences() {
    logger.debug("HTTP GET {}", ApiRoute.USER_PREFERENCES.getPath());
    return apiClient.get(ApiRoute.USER_PREFERENCES, RemoteUserPreferenceResponse.class)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP GET {} failed: {}", ApiRoute.USER_PREFERENCES.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP GET {} success", ApiRoute.USER_PREFERENCES.getPath());
        });
  }

  public CompletableFuture<RemoteUserPreferenceResponse> updateUserPreferences(RemoteUserPreferenceUpdateRequest request) {
    logger.debug("HTTP PUT {} payload theme={} translation={} language={}", ApiRoute.USER_PREFERENCES.getPath(),
        request.theme(), request.defaultTranslation(), request.language());
    return apiClient.put(ApiRoute.USER_PREFERENCES, request, RemoteUserPreferenceResponse.class)
        .whenComplete((result, error) -> {
          if (error != null) {
            logger.warn("HTTP PUT {} failed: {}", ApiRoute.USER_PREFERENCES.getPath(), error.getMessage());
            return;
          }
          logger.debug("HTTP PUT {} success", ApiRoute.USER_PREFERENCES.getPath());
        });
  }
}
