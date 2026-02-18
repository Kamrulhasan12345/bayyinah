package com.ks.bayyinah.infra.local.repository;

import com.ks.bayyinah.infra.local.database.DatabaseManager;

import java.sql.Connection;

/**
 * @deprecated Use {@link LocalRepository} instead.
 */
@Deprecated
public class LocalRespository {
  public Connection getConnection() {
    return DatabaseManager.getConnection();
  }
}

/**
 * Correctly spelled repository class kept for new code.
 * <p>
 * Extends {@link LocalRespository} for full backward compatibility.
 */
class LocalRepository extends LocalRespository {
  // No additional behavior; inherits getConnection().
}
