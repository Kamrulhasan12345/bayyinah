package com.ks.bayyinah.infra.local.repository.user;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.infra.hybrid.model.SyncQueueItem;
import com.ks.bayyinah.infra.local.database.DatabaseManager;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SyncQueueRepository {
  // This repository will manage the sync queue for user data. It will interact
  // with the local database to save and retrieve sync tasks, and also manage
  // the state of the sync process.

  // For simplicity, let's assume a sync task has an ID, a type (e.g., "bookmark",
  // "reading_progress", "user_preference"), a payload (the data to be synced),
  // and
  // a status (e.g., "pending", "in_progress", "completed", "failed").

  // We will need methods to:
  // - Add a sync task to the queue
  // - Get all pending sync tasks
  // - Update the status of a sync task
  // - Remove completed or failed sync tasks from the queue

  public void insert(SyncQueueItem item) {
    String sql = "INSERT INTO sync_queue (operation, table_name, record_id, payload, retry_count, last_error) VALUES (?, ?, ?, ?, ?, ?)";
    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      statement.setString(1, item.getOperation());
      statement.setString(2, item.getTableName());
      statement.setLong(3, item.getRecordId());
      statement.setString(4, item.getPayload());
      statement.setInt(5, item.getRetryCount());
      statement.setString(6, item.getLastError());
      statement.executeUpdate();

      try (var keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          item.setId(keys.getLong(1));
        }
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to enqueue sync item", e);
    }
  }

  public List<SyncQueueItem> findPending(int limit) {
    String sql = "SELECT * FROM sync_queue ORDER BY created_at ASC LIMIT ?";
    List<SyncQueueItem> items = new ArrayList<>();
    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setInt(1, limit);
      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          SyncQueueItem item = new SyncQueueItem();
          item.setId(resultSet.getLong("id"));
          item.setOperation(resultSet.getString("operation"));
          item.setTableName(resultSet.getString("table_name"));
          item.setRecordId(resultSet.getLong("record_id"));
          item.setPayload(resultSet.getString("payload"));

          var createdAt = resultSet.getTimestamp("created_at");
          if (createdAt != null) {
            item.setCreatedAt(createdAt.toLocalDateTime());
          }

          item.setRetryCount(resultSet.getInt("retry_count"));
          item.setLastError(resultSet.getString("last_error"));
          items.add(item);
        }
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to load pending sync items", e);
    }
    return items;
  }

  public Optional<SyncQueueItem> findById(Long id) {
    String sql = "SELECT * FROM sync_queue WHERE id = ?";
    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setLong(1, id);
      try (var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          SyncQueueItem item = new SyncQueueItem();
          item.setId(resultSet.getLong("id"));
          item.setOperation(resultSet.getString("operation"));
          item.setTableName(resultSet.getString("table_name"));
          item.setRecordId(resultSet.getLong("record_id"));
          item.setPayload(resultSet.getString("payload"));

          var createdAt = resultSet.getTimestamp("created_at");
          if (createdAt != null) {
            item.setCreatedAt(createdAt.toLocalDateTime());
          }

          item.setRetryCount(resultSet.getInt("retry_count"));
          item.setLastError(resultSet.getString("last_error"));
          return Optional.of(item);
        }
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to load sync item by ID", e);
    }
    return Optional.empty();
  }

  public void markFailed(Long id, int retryCount, String lastError) {
    String sql = "UPDATE sync_queue SET retry_count = ?, last_error = ? WHERE id = ?";
    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setInt(1, retryCount);
      if (lastError != null) {
        statement.setString(2, lastError);
      } else {
        statement.setNull(2, Types.VARCHAR);
      }
      statement.setLong(3, id);
      statement.executeUpdate();
    } catch (Exception e) {
      throw new RepositoryException("Failed to mark sync item as failed", e);
    }
  }

  public void delete(Long id) {
    String sql = "DELETE FROM sync_queue WHERE id = ?";
    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setLong(1, id);
      statement.executeUpdate();
    } catch (Exception e) {
      throw new RepositoryException("Failed to delete sync item", e);
    }
  }

}
