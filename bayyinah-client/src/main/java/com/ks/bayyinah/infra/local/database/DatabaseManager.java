package com.ks.bayyinah.infra.local.database;

import com.ks.bayyinah.infra.hybrid.model.MainConfig;
import com.ks.bayyinah.infra.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseManager {

  private static HikariDataSource quranPool;
  private static HikariDataSource userPool;

  private DatabaseManager() {
  }

  /**
   * Initialize both databases from config
   */
  public static synchronized void initialize(MainConfig config) {
    if (quranPool == null) {
      quranPool = createPool("quran", config.getQuran().getDatabasePath());
    }
    if (userPool == null) {
      userPool = createPool("user", config.getUser().getDatabasePath());
    }

    // Configure SQLite once after pools are created
    configureSqlite(quranPool);
    configureSqlite(userPool);

    // Shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::closeAll));
  }

  private static HikariDataSource createPool(String name, String path) {
    HikariConfig config = new HikariConfig();
    config.setPoolName(name + "Pool");
    config.setJdbcUrl("jdbc:sqlite:" + path);
    config.setDriverClassName("org.sqlite.JDBC");
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setConnectionTimeout(30_000);
    config.setMaxLifetime(1_800_000);
    return new HikariDataSource(config);
  }

  private static void configureSqlite(HikariDataSource pool) {
    try (Connection conn = pool.getConnection();
        var stmt = conn.createStatement()) {
      stmt.execute("PRAGMA journal_mode=WAL;");
      stmt.execute("PRAGMA busy_timeout=5000;");
      stmt.execute("PRAGMA foreign_keys=ON;");
    } catch (SQLException e) {
      throw new DatabaseException("Failed to configure SQLite", e);
    }
  }

  /**
   * Get connection from Quran database
   */
  public static Connection getQuranConnection() {
    return getConnection(quranPool, "quran");
  }

  /**
   * Get connection from User database
   */
  public static Connection getUserConnection() {
    return getConnection(userPool, "user");
  }

  /**
   * Get DataSource for Quran database (for repository injection)
   */
  public static DataSource getQuranDataSource() {
    return quranPool;
  }

  /**
   * Get DataSource for User database (for repository injection)
   */
  public static DataSource getUserDataSource() {
    return userPool;
  }

  private static Connection getConnection(HikariDataSource pool, String name) {
    if (pool == null || pool.isClosed()) {
      throw new DatabaseException(name + " database is not initialized");
    }
    try {
      return pool.getConnection();
    } catch (SQLException e) {
      throw new DatabaseException("Failed to get " + name + " connection", e);
    }
  }

  public static void closeAll() {
    if (quranPool != null && !quranPool.isClosed())
      quranPool.close();
    if (userPool != null && !userPool.isClosed())
      userPool.close();
  }

  public static boolean isInitialized() {
    return quranPool != null && !quranPool.isClosed() &&
        userPool != null && !userPool.isClosed();
  }
}
