package com.ks.bayyinah.infra.local.repository.user;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.infra.hybrid.model.Bookmark;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookmarksRepository {

  public void insert(Bookmark bookmark) {
    String sql = "INSERT INTO bookmarks (surah_number, ayah_number, title, color, created_at, synced, server_id, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      statement.setInt(1, bookmark.getSurahNumber());
      statement.setInt(2, bookmark.getAyahNumber());
      statement.setString(3, bookmark.getTitle());
      statement.setString(4, bookmark.getColor());
      statement.setTimestamp(5, Timestamp.valueOf(bookmark.getCreatedAt()));
      statement.setBoolean(6, bookmark.isSynced());

      if (bookmark.getServerId() != null) {
        statement.setLong(7, bookmark.getServerId());
      } else {
        statement.setNull(7, Types.BIGINT);
      }

      statement.setBoolean(8, bookmark.isDeleted());

      statement.executeUpdate();

      // Get generated ID
      try (var keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          bookmark.setId(keys.getLong(1));
        }
      }

    } catch (Exception e) {
      throw new RepositoryException("Failed to insert bookmark", e);
    }
  }

  public void update(Bookmark bookmark) {
    String sql = "UPDATE bookmarks SET surah_number = ?, ayah_number = ?, title = ?, color = ?, synced = ?, server_id = ?, deleted = ? WHERE id = ?";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setInt(1, bookmark.getSurahNumber());
      statement.setInt(2, bookmark.getAyahNumber());
      statement.setString(3, bookmark.getTitle());
      statement.setString(4, bookmark.getColor());
      statement.setBoolean(5, bookmark.isSynced());

      if (bookmark.getServerId() != null) {
        statement.setLong(6, bookmark.getServerId());
      } else {
        statement.setNull(6, Types.BIGINT);
      }

      statement.setBoolean(7, bookmark.isDeleted());
      statement.setLong(8, bookmark.getId());

      statement.executeUpdate();

    } catch (Exception e) {
      throw new RepositoryException("Failed to update bookmark with ID: " + bookmark.getId(), e);
    }
  }

  public void delete(Long id) {
    String sql = "DELETE FROM bookmarks WHERE id = ?";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setLong(1, id);
      statement.executeUpdate();

    } catch (Exception e) {
      throw new RepositoryException("Failed to delete bookmark with ID: " + id, e);
    }
  }

  public void softDelete(Long id) {
    String sql = "UPDATE bookmarks SET deleted = 1, synced = 0 WHERE id = ?";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setLong(1, id);
      statement.executeUpdate();

    } catch (Exception e) {
      throw new RepositoryException("Failed to soft delete bookmark with ID: " + id, e);
    }
  }

  public Optional<Bookmark> findById(Long id) {
    String sql = "SELECT * FROM bookmarks WHERE id = ?";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setLong(1, id);

      try (var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(mapToBookmark(resultSet));
        }
      }

    } catch (Exception e) {
      throw new RepositoryException("Failed to find bookmark with ID: " + id, e);
    }

    return Optional.empty();
  }

  public List<Bookmark> findAll() {
    String sql = "SELECT * FROM bookmarks WHERE deleted = 0 ORDER BY created_at DESC";

    List<Bookmark> bookmarks = new ArrayList<>();

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery(sql)) {

      while (resultSet.next()) {
        bookmarks.add(mapToBookmark(resultSet));
      }

    } catch (Exception e) {
      throw new RepositoryException("Failed to fetch all bookmarks", e);
    }

    return bookmarks;
  }

  public List<Bookmark> findBySurah(int surahNumber) {
    String sql = "SELECT * FROM bookmarks WHERE surah_number = ? AND deleted = 0 ORDER BY ayah_number";

    List<Bookmark> bookmarks = new ArrayList<>();

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setInt(1, surahNumber);

      try (var resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          bookmarks.add(mapToBookmark(resultSet));
        }
      }

    } catch (Exception e) {
      throw new RepositoryException("Failed to fetch bookmarks for surah: " + surahNumber, e);
    }

    return bookmarks;
  }

  public Optional<Bookmark> findByVerse(int surahNumber, int ayahNumber) {
    String sql = "SELECT * FROM bookmarks WHERE surah_number = ? AND ayah_number = ? AND deleted = 0";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setInt(1, surahNumber);
      statement.setInt(2, ayahNumber);

      try (var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(mapToBookmark(resultSet));
        }
      }

    } catch (Exception e) {
      throw new RepositoryException(
          "Failed to find bookmark for verse " + surahNumber + ":" + ayahNumber, e);
    }

    return Optional.empty();
  }

  public List<Bookmark> findUnsynced() {
    String sql = "SELECT * FROM bookmarks WHERE synced = 0 ORDER BY created_at";

    List<Bookmark> bookmarks = new ArrayList<>();

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery(sql)) {

      while (resultSet.next()) {
        bookmarks.add(mapToBookmark(resultSet));
      }

    } catch (Exception e) {
      throw new RepositoryException("Failed to fetch unsynced bookmarks", e);
    }

    return bookmarks;
  }

  public boolean existsByVerse(int surahNumber, int ayahNumber) {
    String sql = "SELECT COUNT(*) FROM bookmarks WHERE surah_number = ? AND ayah_number = ? AND deleted = 0";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setInt(1, surahNumber);
      statement.setInt(2, ayahNumber);

      try (var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1) > 0;
        }
      }

    } catch (Exception e) {
      throw new RepositoryException(
          "Failed to check bookmark existence for verse " + surahNumber + ":" + ayahNumber, e);
    }

    return false;
  }

  public long count() {
    String sql = "SELECT COUNT(*) FROM bookmarks WHERE deleted = 0";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery(sql)) {

      if (resultSet.next()) {
        return resultSet.getLong(1);
      }

    } catch (Exception e) {
      throw new RepositoryException("Failed to count bookmarks", e);
    }

    return 0;
  }

  public void deleteAll() {
    String sql = "DELETE FROM bookmarks";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.createStatement()) {

      statement.execute(sql);

    } catch (Exception e) {
      throw new RepositoryException("Failed to delete all bookmarks", e);
    }
  }

  public void markAsSynced(Long id, Long serverId) {
    String sql = "UPDATE bookmarks SET synced = 1, server_id = ? WHERE id = ?";

    try (var connection = DatabaseManager.getUserConnection();
        var statement = connection.prepareStatement(sql)) {

      statement.setLong(1, serverId);
      statement.setLong(2, id);
      statement.executeUpdate();

    } catch (Exception e) {
      throw new RepositoryException("Failed to mark bookmark as synced: " + id, e);
    }
  }

  /**
   * Map ResultSet to Bookmark model
   */
  private Bookmark mapToBookmark(ResultSet rs) throws SQLException {
    Bookmark bookmark = new Bookmark();
    bookmark.setId(rs.getLong("id"));
    bookmark.setSurahNumber(rs.getInt("surah_number"));
    bookmark.setAyahNumber(rs.getInt("ayah_number"));
    bookmark.setTitle(rs.getString("title"));
    bookmark.setColor(rs.getString("color"));

    Timestamp createdAt = rs.getTimestamp("created_at");
    if (createdAt != null) {
      bookmark.setCreatedAt(createdAt.toLocalDateTime());
    }

    bookmark.setSynced(rs.getBoolean("synced"));

    long serverId = rs.getLong("server_id");
    if (!rs.wasNull()) {
      bookmark.setServerId(serverId);
    }

    bookmark.setDeleted(rs.getBoolean("deleted"));

    return bookmark;
  }
}
