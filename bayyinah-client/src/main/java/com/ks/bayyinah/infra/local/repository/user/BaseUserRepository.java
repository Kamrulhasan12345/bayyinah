package com.ks.bayyinah.infra.local.repository.user;

import java.sql.Connection;

import com.ks.bayyinah.infra.local.database.DatabaseManager;

public class BaseUserRepository {
  protected final DatabaseManager db;

  public BaseUserRepository(DatabaseManager db) {
    this.db = db;
  }

  public Connection getConnection() {
    return db.getUserConnection();
  }
}
