package com.ks.bayyinah.infra.local.repository.user;

import java.sql.SQLException;
import java.sql.Timestamp;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.infra.hybrid.model.User;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

public class UserRepository {

  public User get() {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement("SELECT * FROM users WHERE id = 1")) {
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            User user = User.builder()
                .id(resultSet.getLong("id"))
                .serverId(resultSet.getObject("server_id", Long.class))
                .username(resultSet.getString("username"))
                .email(resultSet.getString("email"))
                .firstName(resultSet.getString("first_name"))
                .lastName(resultSet.getString("last_name"))
                .deviceId(resultSet.getString("device_id"))
                .isGuest(resultSet.getBoolean("is_guest"))
                .lastSyncAt(resultSet.getTimestamp("last_sync_at").toLocalDateTime())
                .createdAt(resultSet.getTimestamp("created_at").toLocalDateTime())
                .build();
            return user;
          }
        }
      }
    } catch (SQLException e) {
      // Handle exception, possibly return a guest user or null
      e.printStackTrace();
      throw new RepositoryException("Failed to fetch current user", e);
    }
    return null;
  }

  public void insert(User user) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "INSERT INTO users (id, server_id, username, email, first_name, last_name, device_id, is_guest, last_sync_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
        statement.setLong(1, user.getId());
        statement.setObject(2, user.getServerId()); // Use setObject for nullable Long
        statement.setString(3, user.getUsername());
        statement.setString(4, user.getEmail());
        statement.setString(5, user.getFirstName());
        statement.setString(6, user.getLastName());
        statement.setString(7, user.getDeviceId());
        statement.setBoolean(8, user.isGuest());
        statement.setTimestamp(9, Timestamp.valueOf(user.getLastSyncAt()));
        statement.setTimestamp(10, Timestamp.valueOf(user.getCreatedAt()));
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to save user", e);
    }
  }

  public void update(User user) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "UPDATE users SET server_id = ?, username = ?, email = ?, first_name = ?, last_name = ?, device_id = ?, is_guest = ?, last_sync_at = ?, created_at = ? WHERE id = ?")) {
        statement.setObject(1, user.getServerId()); // Use setObject for nullable Long
        statement.setString(2, user.getUsername());
        statement.setString(3, user.getEmail());
        statement.setString(4, user.getFirstName());
        statement.setString(5, user.getLastName());
        statement.setString(6, user.getDeviceId());
        statement.setBoolean(7, user.isGuest());
        statement.setTimestamp(8, Timestamp.valueOf(user.getLastSyncAt()));
        statement.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));
        statement.setLong(10, user.getId());
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to update user", e);
    }
  }

  // public User createGuestUser() {
  // return User.createGuest();
  // }
  //
  // public void promoteGuestToRegistered(String username, String email, Long
  // serverId) {
  // User currentUser = get();
  // currentUser.promoteToRegistered(username, email, serverId);
  //
  // // maybe save then?
  // }

  public boolean isGuestMode() {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement("SELECT is_guest FROM users WHERE id = 1")) {
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getBoolean("is_guest");
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to check guest mode", e);
    }
    return true;
  }

  public boolean canSync() {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement("SELECT is_guest, server_id FROM users WHERE id = 1")) {
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            boolean isGuest = resultSet.getBoolean("is_guest");
            Long serverId = resultSet.getObject("server_id", Long.class);
            return !isGuest && serverId != null;
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to check sync capability", e);
    }
    return false;
  }

  public void clear() {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement("DELETE FROM users")) {
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to clear user data", e);
    }
  }

}
