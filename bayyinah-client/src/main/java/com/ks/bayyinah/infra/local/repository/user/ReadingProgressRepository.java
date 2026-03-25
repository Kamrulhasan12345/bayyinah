package com.ks.bayyinah.infra.local.repository.user;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.infra.hybrid.model.ReadingProgress;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

public class ReadingProgressRepository {
  public void insert(ReadingProgress progress) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "INSERT INTO reading_progress (surah_number, ayah_number, last_read_at, synced, server_id, deleted) VALUES (?, ?, ?, ?, ?, ?)",
              Statement.RETURN_GENERATED_KEYS)) {

        statement.setInt(1, progress.getSurahNumber());
        statement.setInt(2, progress.getAyahNumber());
        statement.setTimestamp(3, Timestamp.valueOf(progress.getLastReadAt()));
        statement.setBoolean(4, progress.isSynced());

        if (progress.getServerId() != null) {
          statement.setLong(5, progress.getServerId());
        } else {
          statement.setNull(5, Types.BIGINT);
        }

        statement.setBoolean(6, progress.isDeleted());

        statement.executeUpdate();

        // Get generated ID
        try (var keys = statement.getGeneratedKeys()) {
          if (keys.next()) {
            progress.setId(keys.getLong(1));
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to insert reading progress", e);
    }
  }

  public void update(ReadingProgress progress) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "UPDATE reading_progress SET surah_number = ?, ayah_number = ?, last_read_at = ?, time_spent_seconds = ?, synced = ?, server_id = ?, deleted = ? WHERE id = ?")) {

        statement.setInt(1, progress.getSurahNumber());
        statement.setInt(2, progress.getAyahNumber());
        statement.setTimestamp(3, Timestamp.valueOf(progress.getLastReadAt()));
        statement.setInt(4, progress.getTimeSpentSeconds());
        statement.setBoolean(5, progress.isSynced());

        if (progress.getServerId() != null) {
          statement.setLong(6, progress.getServerId());
        } else {
          statement.setNull(6, Types.BIGINT);
        }

        statement.setBoolean(7, progress.isDeleted());
        statement.setLong(8, progress.getId());

        statement.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to update reading progress", e);
    }
  }

  public ReadingProgress findBySurah(int surahNumber) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM reading_progress WHERE surah_number = ? AND deleted = 0 ORDER BY last_read_at DESC LIMIT 1")) {

        statement.setInt(1, surahNumber);

        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            ReadingProgress progress = new ReadingProgress();
            progress.setId(resultSet.getLong("id"));
            progress.setSurahNumber(resultSet.getInt("surah_number"));
            progress.setAyahNumber(resultSet.getInt("ayah_number"));
            progress.setLastReadAt(resultSet.getTimestamp("last_read_at").toLocalDateTime());
            progress.setTimeSpentSeconds(resultSet.getInt("time_spent_seconds"));
            progress.setSynced(resultSet.getBoolean("synced"));

            long serverId = resultSet.getLong("server_id");
            if (!resultSet.wasNull()) {
              progress.setServerId(serverId);
            }

            progress.setDeleted(resultSet.getBoolean("deleted"));
            return progress;
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to find reading progress for " + surahNumber, e);
    }
    return null;
  }

  public Optional<ReadingProgress> findByVerse(int surahNumber, int ayahNumber) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM reading_progress WHERE surah_number = ? AND ayah_number = ? AND deleted = 0 LIMIT 1")) {

        statement.setInt(1, surahNumber);
        statement.setInt(2, ayahNumber);

        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            ReadingProgress progress = new ReadingProgress();
            progress.setId(resultSet.getLong("id"));
            progress.setSurahNumber(resultSet.getInt("surah_number"));
            progress.setAyahNumber(resultSet.getInt("ayah_number"));
            progress.setLastReadAt(resultSet.getTimestamp("last_read_at").toLocalDateTime());
            progress.setTimeSpentSeconds(resultSet.getInt("time_spent_seconds"));
            progress.setSynced(resultSet.getBoolean("synced"));

            long serverId = resultSet.getLong("server_id");
            if (!resultSet.wasNull()) {
              progress.setServerId(serverId);
            }

            progress.setDeleted(resultSet.getBoolean("deleted"));
            return Optional.of(progress);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException(
          "Failed to find reading progress for verse " + surahNumber + ":" + ayahNumber, e);
    }
    return Optional.empty();
  }

  public Optional<ReadingProgress> findLatest() {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM reading_progress WHERE deleted = 0 ORDER BY last_read_at DESC LIMIT 1")) {

        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            ReadingProgress progress = new ReadingProgress();
            progress.setId(resultSet.getLong("id"));
            progress.setSurahNumber(resultSet.getInt("surah_number"));
            progress.setAyahNumber(resultSet.getInt("ayah_number"));
            progress.setLastReadAt(resultSet.getTimestamp("last_read_at").toLocalDateTime());
            progress.setTimeSpentSeconds(resultSet.getInt("time_spent_seconds"));
            progress.setSynced(resultSet.getBoolean("synced"));

            long serverId = resultSet.getLong("server_id");
            if (!resultSet.wasNull()) {
              progress.setServerId(serverId);
            }

            progress.setDeleted(resultSet.getBoolean("deleted"));
            return Optional.of(progress);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to retrieve latest reading progress", e);
    }
    return Optional.empty();
  }

  public List<ReadingProgress> findUnsynced() {
    List<ReadingProgress> progressList = new ArrayList<>();
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM reading_progress WHERE synced = 0 AND deleted = 0 ORDER BY last_read_at DESC")) {

        try (var resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            ReadingProgress progress = new ReadingProgress();
            progress.setId(resultSet.getLong("id"));
            progress.setSurahNumber(resultSet.getInt("surah_number"));
            progress.setAyahNumber(resultSet.getInt("ayah_number"));
            progress.setLastReadAt(resultSet.getTimestamp("last_read_at").toLocalDateTime());
            progress.setTimeSpentSeconds(resultSet.getInt("time_spent_seconds"));
            progress.setSynced(resultSet.getBoolean("synced"));

            long serverId = resultSet.getLong("server_id");
            if (!resultSet.wasNull()) {
              progress.setServerId(serverId);
            }

            progress.setDeleted(resultSet.getBoolean("deleted"));
            progressList.add(progress);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to retrieve unsynced reading progress", e);
    }
    return progressList;
  }

  public void markAsSynced(Long id, Long serverId) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "UPDATE reading_progress SET synced = 1, server_id = ? WHERE id = ?")) {

        if (serverId != null) {
          statement.setLong(1, serverId);
        } else {
          statement.setNull(1, Types.BIGINT);
        }
        statement.setLong(2, id);
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to mark reading progress as synced", e);
    }
  }

  public List<ReadingProgress> findAll() {
    List<ReadingProgress> progressList = new ArrayList<>();
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM reading_progress WHERE deleted = 0 ORDER BY last_read_at DESC")) {

        try (var resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            ReadingProgress progress = new ReadingProgress();
            progress.setId(resultSet.getLong("id"));
            progress.setSurahNumber(resultSet.getInt("surah_number"));
            progress.setAyahNumber(resultSet.getInt("ayah_number"));
            progress.setLastReadAt(resultSet.getTimestamp("last_read_at").toLocalDateTime());
            progress.setTimeSpentSeconds(resultSet.getInt("time_spent_seconds"));
            progress.setSynced(resultSet.getBoolean("synced"));

            long serverId = resultSet.getLong("server_id");
            if (!resultSet.wasNull()) {
              progress.setServerId(serverId);
            }

            progress.setDeleted(resultSet.getBoolean("deleted"));
            progressList.add(progress);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to retrieve reading progress list", e);
    }
    return progressList;
  }
}
