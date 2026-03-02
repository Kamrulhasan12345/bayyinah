package com.ks.bayyinah.infra.local.repository.user;

import java.sql.SQLException;
import java.util.Optional;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

public class AuthTokensRepository {

  public void insertOrUpdate(AuthTokens authTokens) {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement(
              "INSERT INTO auth_tokens (id, access_token, refresh_token, expires_at) VALUES (1, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET access_token = excluded.access_token, refresh_token = excluded.refresh_token, expires_at = excluded.expires_at")) {
        statement.setString(1, authTokens.getAccessToken());
        statement.setString(2, authTokens.getRefreshToken());
        statement.setTimestamp(3, java.sql.Timestamp.valueOf(authTokens.getExpiresAt()));
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to save auth tokens", e);
    }
  }

  public Optional<AuthTokens> get() {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement("SELECT * FROM auth_tokens WHERE id = 1")) {
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            AuthTokens authTokens = new AuthTokens(
                resultSet.getString("access_token"),
                resultSet.getString("refresh_token"),
                resultSet.getTimestamp("expires_at").toLocalDateTime());
            return Optional.of(authTokens);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to fetch auth tokens", e);
    }
    return Optional.empty();
  }

  public boolean isExpired() {
    Optional<AuthTokens> tokensOpt = get();
    if (tokensOpt.isPresent()) {
      AuthTokens tokens = tokensOpt.get();
      return tokens.isExpired();
    }
    return true; // If no tokens, consider it expired
  }

  public void clear() {
    try {
      try (var connection = DatabaseManager.getUserConnection();
          var statement = connection.prepareStatement("DELETE FROM auth_tokens WHERE id = 1")) {
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RepositoryException("Failed to clear auth tokens", e);
    }
  }

}
