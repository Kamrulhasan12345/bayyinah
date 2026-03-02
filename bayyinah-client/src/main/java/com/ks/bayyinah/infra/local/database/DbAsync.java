package com.ks.bayyinah.infra.local.database;

import javafx.application.Platform;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;

public final class DbAsync {
  private DbAsync() {
  }

  public static <T> void runWithUi(Supplier<T> dbTask, Consumer<T> onSuccess, Consumer<Throwable> onError) {
    Thread.startVirtualThread(() -> {
      try {
        T result = dbTask.get();
        Platform.runLater(() -> onSuccess.accept(result));
      } catch (Exception e) {
        Platform.runLater(() -> onError.accept(e));
        return;
      }
    });
  }

  public static <T> void runWithUi(Supplier<T> dbTask, Consumer<T> onSuccess) {
    runWithUi(dbTask, onSuccess, e -> {
      System.err.println("Database operation failed: " + e.getMessage());
      e.printStackTrace();
    });
  }

  public static void run(Runnable dbOperation) {
    Thread.startVirtualThread(dbOperation);
  }

  public static <T> CompletableFuture<T> supplyAsync(Supplier<T> dbOperation) {
    return CompletableFuture.supplyAsync(dbOperation, Thread::startVirtualThread);
  }

  @SafeVarargs
  public static CompletableFuture<Void> allOfAsync(Supplier<?>... operations) {
    CompletableFuture<?>[] futures = new CompletableFuture[operations.length];
    for (int i = 0; i < operations.length; i++) {
      futures[i] = supplyAsync(operations[i]);
    }
    return CompletableFuture.allOf(futures);
  }
}
