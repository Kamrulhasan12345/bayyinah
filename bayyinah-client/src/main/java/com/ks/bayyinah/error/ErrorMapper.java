package com.ks.bayyinah.error;

import com.ks.bayyinah.infra.exception.UnauthorizedException;
import com.ks.bayyinah.infra.remote.client.ApiClient.ApiException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;

public class ErrorMapper {
    
    /**
     * Map exception to error category
     */
    public static ErrorCategory mapException(Throwable throwable) {
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
        
        // Authentication errors
        if (throwable instanceof UnauthorizedException) {
            if (throwable.getMessage().contains("token expired")) {
                return ErrorCategory.AUTH_TOKEN_EXPIRED;
            }
            return ErrorCategory.AUTH_UNAUTHORIZED;
        }
        
        // API errors
        if (throwable instanceof ApiException) {
            ApiException apiEx = (ApiException) throwable;
            String message = apiEx.getMessage();
            
            if (message.contains("401")) {
                return ErrorCategory.AUTH_INVALID_CREDENTIALS;
            }
            if (message.contains("403")) {
                return ErrorCategory.AUTH_UNAUTHORIZED;
            }
            if (message.contains("404")) {
                return ErrorCategory.DATA_NOT_FOUND;
            }
            if (message.contains("500") || message.contains("502") || message.contains("503")) {
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
}
