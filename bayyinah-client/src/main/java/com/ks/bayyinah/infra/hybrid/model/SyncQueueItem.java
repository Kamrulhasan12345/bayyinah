package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SyncQueueItem {

  private Long id;
  private String operation; // "CREATE", "UPDATE", "DELETE"
  private String tableName; // "bookmarks", "notes", etc.
  private Long recordId; // Local ID of the record
  private String payload; // JSON representation of the data
  private LocalDateTime createdAt;
  private int retryCount;
  private String lastError;

  // Constructors
  public SyncQueueItem(String operation, String tableName, Long recordId, String payload) {
    this.operation = operation;
    this.tableName = tableName;
    this.recordId = recordId;
    this.payload = payload;
    this.createdAt = LocalDateTime.now();
    this.retryCount = 0;
  }

  // Getters and Setters
  public void incrementRetry() {
    this.retryCount++;
  }

  public boolean hasExceededRetries() {
    return retryCount >= 3; // Max 3 retries
  }
}
