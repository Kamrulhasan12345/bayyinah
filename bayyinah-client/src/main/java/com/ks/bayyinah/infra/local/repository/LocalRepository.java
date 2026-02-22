package com.ks.bayyinah.infra.local.repository;

import com.ks.bayyinah.infra.local.database.DatabaseManager;

import java.sql.Connection;

public class LocalRepository {
  // No additional behavior; inherits getConnection().
  public Connection getConnection() {
    return DatabaseManager.getConnection();
  }
}
