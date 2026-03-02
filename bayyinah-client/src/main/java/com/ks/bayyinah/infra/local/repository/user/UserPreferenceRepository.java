package com.ks.bayyinah.infra.local.repository.user;

import java.util.ArrayList;
import java.util.List;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.infra.hybrid.model.UserPreference;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

public class UserPreferenceRepository {
  public void insertOrUpdate(String key, String value) {
    try {
      try (var conn = DatabaseManager.getUserConnection();
          var stmt = conn.prepareStatement(
              "INSERT INTO user_preferences (key, value) VALUES (?, ?) " +
                  "ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
        stmt.setString(1, key);
        stmt.setString(2, value);
        stmt.executeUpdate();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to insert/update user preference for key: " + key, e);
    }
  }

  public UserPreference get(String key) {
    try {
      try (var conn = DatabaseManager.getUserConnection();
          var stmt = conn.prepareStatement("SELECT key, value FROM user_preferences WHERE key = ?")) {
        stmt.setString(1, key);
        try (var rs = stmt.executeQuery()) {
          if (rs.next()) {
            return new UserPreference(rs.getString("key"), rs.getString("value"));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to fetch user preference for key: " + key, e);
    }
    return null;
  }

  public List<UserPreference> findAll() {
    List<UserPreference> prefs = new ArrayList<>();
    try {
      try (var conn = DatabaseManager.getUserConnection();
          var stmt = conn.prepareStatement("SELECT key, value FROM user_preferences");
          var rs = stmt.executeQuery()) {
        while (rs.next()) {
          prefs.add(new UserPreference(rs.getString("key"), rs.getString("value")));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to fetch user preferences", e);
    }
    return prefs;
  }
}
