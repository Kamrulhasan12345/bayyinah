package com.ks.bayyinah.infra.local.database;

import com.ks.bayyinah.infra.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseManager {

  private static HikariDataSource dataSource;
  private static String currentDatabasePath;

  public static synchronized void initialize(String databasePath) {
    if (dataSource != null && !dataSource.isClosed()) {
      // if already initialized with same path, don't initialize again
      if (databasePath.equals(currentDatabasePath)) {
        return;
      }

      close();
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + databasePath);
    config.setDriverClassName("org.sqlite.JDBC");

    // Connection pool setting optimized for SQLite
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);

    // Connection test query for SQLite
    config.setConnectionTestQuery("SELECT 1");

    // Pool name for logging/debugging
    config.setPoolName("BayyinahPool");

    dataSource = new HikariDataSource(config);
    currentDatabasePath = databasePath;
  }

  public static Connection getConnection() {
    if (dataSource == null || dataSource.isClosed()) {
      throw new DatabaseException("Database is not initialized. Call DatabaseManager.initialize() first.");
    }

    try {
      return dataSource.getConnection();
    } catch (SQLException e) {
      throw new DatabaseException("Failed to get database connection.", e);
    }
  }

  public static DataSource getDataSource() {
    if (dataSource == null || dataSource.isClosed()) {
      throw new DatabaseException("Database is not initialized. Call DatabaseManager.initialize() first.");
    }
    return dataSource;
  }

  public static boolean isInitialized() {
    return dataSource != null && !dataSource.isClosed();
  }

  public static synchronized void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      dataSource = null;
      currentDatabasePath = null;
    }
  }

  public static String getCurrentDatabasePath() {
    return currentDatabasePath;
  }
}
