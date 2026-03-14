package com.ks.bayyinah.error;

import com.ks.bayyinah.ui.ToastSeverity;

public enum ErrorCategory {

  // Network errors
  NETWORK_CONNECTION_FAILED("Connection Error", "Unable to connect to server. Working offline.", ToastSeverity.WARNING),
  NETWORK_TIMEOUT("Connection Timeout", "Request took too long. Please try again.", ToastSeverity.WARNING),

  // Authentication errors
  AUTH_INVALID_CREDENTIALS("Login Failed", "Invalid username or password.", ToastSeverity.ERROR),
  AUTH_TOKEN_EXPIRED("Session Expired", "Please log in again.", ToastSeverity.WARNING),
  AUTH_UNAUTHORIZED("Access Denied", "You don't have permission for this action.", ToastSeverity.ERROR),

  // Data errors
  DATA_NOT_FOUND("Not Found", "The requested data could not be found.", ToastSeverity.WARNING),
  DATA_VALIDATION_FAILED("Invalid Input", "Please check your input and try again.", ToastSeverity.ERROR),
  DATA_SYNC_FAILED("Sync Failed", "Could not sync with cloud. Data saved locally.", ToastSeverity.WARNING),

  // Server errors
  SERVER_ERROR("Server Error", "Something went wrong on our end. Please try again.", ToastSeverity.ERROR),
  SERVER_UNAVAILABLE("Service Unavailable", "Server is temporarily unavailable.", ToastSeverity.WARNING),

  // Database errors
  DATABASE_ERROR("Database Error", "Failed to save data. Please try again.", ToastSeverity.ERROR),
  DATABASE_CONNECTION_FAILED("Database Unavailable", "Local database is unavailable.", ToastSeverity.CRITICAL),

  // WebSocket errors
  WEBSOCKET_CONNECTION_FAILED("Connection Lost", "Disconnected from session. Reconnecting...", ToastSeverity.WARNING),
  WEBSOCKET_MESSAGE_FAILED("Message Failed", "Failed to send message. Please try again.", ToastSeverity.ERROR),

  // Generic errors
  UNKNOWN_ERROR("Unexpected Error", "An unexpected error occurred. Please try again.", ToastSeverity.ERROR);

  private final String title;
  private final String message;
  private final ToastSeverity severity;

  ErrorCategory(String title, String message, ToastSeverity severity) {
    this.title = title;
    this.message = message;
    this.severity = severity;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public ToastSeverity getSeverity() {
    return severity;
  }
}
