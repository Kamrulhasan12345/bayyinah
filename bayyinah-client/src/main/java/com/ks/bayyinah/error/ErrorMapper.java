package com.ks.bayyinah.error;

import com.ks.bayyinah.infra.exception.UnauthorizedException;
import com.ks.bayyinah.infra.remote.client.ApiClient.ApiException;

import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ErrorMapper {

  /**
   * Map exception to error category
   */
  public static ErrorCategory mapException(Throwable throwable) {
    throwable = unwrap(throwable);

    // Unwrap CompletionException (from CompletableFuture)
    if (throwable instanceof CompletionException) {
      throwable = throwable.getCause();
    }

    // Network errors
    if (throwable instanceof ConnectException) {
      return ErrorCategory.NETWORK_CONNECTION_FAILED;
    }
    if (throwable instanceof SocketTimeoutException) {
      return ErrorCategory.NETWORK_TIMEOUT;
    }
    if (throwable instanceof SocketException) {
      // Catch-all for other socket issues
      // String msg = throwable.getMessage();
      // if (msg != null) {
      //   msg = msg.toLowerCase();
      //   if (msg.contains("reset") || msg.contains("abort")) {
      //     return ErrorCategory.NETWORK_CONNECTION_FAILED; // "Connection lost"
      //   }
      //   if (msg.contains("unreachable")) {
      //     return ErrorCategory.NETWORK_CONNECTION_FAILED; // "No network"
      //   }
      // }
      return ErrorCategory.NETWORK_CONNECTION_FAILED; // Generic socket error
    }

    // Authentication errors
    if (throwable instanceof UnauthorizedException) {
      String message = throwable.getMessage();
      if (message != null && message.contains("token expired")) {
        return ErrorCategory.AUTH_TOKEN_EXPIRED;
      }
      return ErrorCategory.AUTH_UNAUTHORIZED;
    }

    // API errors
    if (throwable instanceof ApiException) {
      ApiException apiEx = (ApiException) throwable;
      String message = apiEx.getMessage();

      if (message != null && message.contains("401")) {
        return ErrorCategory.AUTH_INVALID_CREDENTIALS;
      }
      if (message != null && message.contains("403")) {
        return ErrorCategory.AUTH_UNAUTHORIZED;
      }
      if (message != null && message.contains("404")) {
        return ErrorCategory.DATA_NOT_FOUND;
      }
      if (message != null
          && (message.contains("500") || message.contains("502") || message.contains("503"))) {
        return ErrorCategory.SERVER_ERROR;
      }
    }

    // Database errors
    if (throwable instanceof SQLException) {
      return ErrorCategory.DATABASE_ERROR;
    }

    // Default
    return ErrorCategory.UNKNOWN_ERROR;
  }

  /**
   * Get user-friendly message with context
   */
  public static String getUserFriendlyMessage(Throwable throwable, String context) {
    ErrorCategory category = mapException(throwable);

    // Add context if provided
    if (context != null && !context.isEmpty()) {
      return category.getMessage() + " (" + context + ")";
    }

    return category.getMessage();
  }

  private static Throwable unwrap(Throwable throwable) {
    Throwable current = throwable;
    while (current instanceof CompletionException
        || current instanceof ExecutionException
        || current instanceof InvocationTargetException) {
      Throwable cause = current.getCause();
      if (cause == null || cause == current)
        break;
      current = cause;
    }
    return current;
  }
}
