package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.hybrid.model.SyncQueueItem;
import com.ks.bayyinah.infra.local.repository.user.SyncQueueRepository;
import java.util.List;
import java.util.Optional;

public class SyncQueueService {
  // This service will manage a queue of actions that need to be synced with the
  // server. It will interact with the local database to save and retrieve the
  // queue, and also handle the logic for syncing with the server when needed.

  // For simplicity, let's assume each action in the queue is represented by a
  // string (e.g., "bookmark:123", "readingProgress:2:5", etc.).

  // We will need methods to:
  // - Add an action to the sync queue
  // - Remove an action from the sync queue
  // - Get all actions in the sync queue
  // - Sync the queue with the server

  // The implementation details will depend on the specific requirements and data
  // models of the application.
  private final SyncQueueRepository repository;

  public SyncQueueService(SyncQueueRepository repository) {
    this.repository = repository;
  }

  public void enqueueUpsert(String tableName, Long recordId, String payload) {
    SyncQueueItem item = new SyncQueueItem("UPSERT", tableName, recordId, payload);
    repository.insert(item);
  }

  public void enqueueDelete(String tableName, Long recordId, String payload) {
    SyncQueueItem item = new SyncQueueItem("DELETE", tableName, recordId, payload);
    repository.insert(item);
  }

  public List<SyncQueueItem> getPending(int limit) {
    return repository.findPending(limit);
  }

  public void markFailed(Long id, String lastError) {
    Optional<SyncQueueItem> itemOpt = repository.findById(id);

    if (itemOpt.isPresent()) {
      SyncQueueItem item = itemOpt.get();
      if (item.hasExceededRetries()) {
        repository.delete(id); // TODO: decide what to do after max retries
        return;
      }
      int retryCount = item.getRetryCount() + 1;
      repository.markFailed(id, retryCount, lastError);
    }
  }

  public void markCompleted(Long id) {
    repository.delete(id);
  }
}
