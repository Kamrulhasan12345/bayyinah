package com.ks.bayyinah.infra.local.database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class DBExecutor {
  private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

  private DBExecutor() {
    // Private constructor to prevent instantiation
  }

  public static void run(Runnable task) {
    executorService.execute(task);
  }

  public static <T> CompletableFuture<T> supply(Supplier<T> task) {
    return CompletableFuture.supplyAsync(task, executorService);
  }

  public static void close() {
    executorService.shutdown();
  }
}
