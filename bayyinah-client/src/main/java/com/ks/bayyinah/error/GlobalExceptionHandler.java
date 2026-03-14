package com.ks.bayyinah.error;

import com.ks.bayyinah.ui.*;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final ToastManager toastManager;

  public GlobalExceptionHandler() {
    this.toastManager = ToastManager.getInstance();
  }

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    handleException(throwable, "Uncaught exception in thread: " + thread.getName());
  }

  /**
   * Handle exception globally
   */
  public void handleException(Throwable throwable, String context) {
    // Log to file
    logException(throwable, context);

    // Map to error category
    ErrorCategory category = ErrorMapper.mapException(throwable);

    // Show toast notification
    Platform.runLater(() -> {
      toastManager.showError(category);
    });

    // For critical errors, ask if user wants to restart
    if (category.getSeverity() == ToastSeverity.CRITICAL) {
      handleCriticalError(throwable);
    }
  }

  /**
   * Log exception to file
   */
  private void logException(Throwable throwable, String context) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    logger.error("=== Exception at {} ===", timestamp);
    logger.error("Context: {}", context);
    logger.error("Exception: ", throwable);

    // Get stack trace
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    logger.error("Stack trace:\n{}", sw.toString());
  }

  /**
   * Handle critical errors
   */
  private void handleCriticalError(Throwable throwable) {
    Platform.runLater(() -> {
      // Show confirmation dialog asking to restart
      javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
          javafx.scene.control.Alert.AlertType.CONFIRMATION);
      alert.setTitle("Critical Error");
      alert.setHeaderText("A critical error has occurred");
      alert.setContentText(
          "The application encountered a critical error and may not function properly.\n\n" +
              "Would you like to restart?");
      alert.getButtonTypes().setAll(
          javafx.scene.control.ButtonType.OK,
          javafx.scene.control.ButtonType.CANCEL
      );

      alert.showAndWait().ifPresent(response -> {
        if (response == javafx.scene.control.ButtonType.OK) {
          restartApplication();
        }
      });
    });
  }

  /**
   * Restart application
   */
  private void restartApplication() {
    // Save any pending data
    // Then restart
    Platform.exit();
    // In production, you'd trigger actual restart via launcher
  }
}
