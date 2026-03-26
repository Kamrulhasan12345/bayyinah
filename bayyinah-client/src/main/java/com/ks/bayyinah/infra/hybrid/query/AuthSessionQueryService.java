package com.ks.bayyinah.infra.hybrid.query;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import com.ks.bayyinah.infra.exception.UnauthorizedException;
import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.hybrid.model.User;
import com.ks.bayyinah.infra.hybrid.service.AuthTokensService;
import com.ks.bayyinah.infra.hybrid.service.SyncOrchestratorService;
import com.ks.bayyinah.infra.hybrid.service.UserService;
import com.ks.bayyinah.infra.remote.dto.auth.TokensResponse;
import com.ks.bayyinah.infra.remote.query.RemoteUserQueryService;

import lombok.*;

@AllArgsConstructor
public class AuthSessionQueryService {
  private final AuthTokensService authTokensService;
  private final UserService userService;
  private final RemoteUserQueryService remoteUserQueryService;
  private final SyncOrchestratorService syncOrchestratorService;

  public enum LogoutReason {
    SUCCESS,
    SESSION_EXPIRED,
    NETWORK_FAILURE,
    UNKNOWN_FAILURE
  }

  public record LogoutResult(
      boolean localSignedOut,
      boolean remoteLogoutSucceeded,
      LogoutReason reason,
      String message) {
  }

  public User ensureGuestSession() {
    var tokensOpt = authTokensService.getAuthTokens();
    if (tokensOpt.isEmpty()) {
      return userService.createGuestUser();
    }

    AuthTokens tokens = tokensOpt.get();
    if (tokens.isExpired() || tokens.isRefreshTokenExpired()) {
      authTokensService.clearAuthTokens();
      userService.clearUser();
      return userService.createGuestUser();
    }

    return null;
  }

  public void login(String username, String password) {
    remoteUserQueryService.login(username, password).thenAccept(loginResponse -> {
      TokensResponse tokensResponse = loginResponse.tokens();
      LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(tokensResponse.getExpiresIn()));
      AuthTokens authTokens = new AuthTokens(tokensResponse.getAccessToken(), tokensResponse.getRefreshToken(),
          expiresAt);
      authTokensService.saveAuthTokens(authTokens);

      User user = User.createRegistered(

          loginResponse.user().id(),
          loginResponse.user().username(),
          loginResponse.user().email(),
          loginResponse.user().firstName(),
          loginResponse.user().lastName());

      userService.saveUser(user);

      if (syncOrchestratorService != null) {
        syncOrchestratorService.runSyncNowAsync();
      }
    }).join();
  }

  public void register(String username, String email, String password, String firstName, String lastName) {
    remoteUserQueryService.register(username, email, password, firstName, lastName).thenAccept(regResponse -> {
      login(username, password);
    }).join();
  }

  public LogoutResult logout() {
    String refreshToken = authTokensService.getAuthTokens()
        .map(AuthTokens::getRefreshToken)
        .orElse(null);

    Throwable remoteFailure = null;
    boolean remoteLogoutSucceeded = false;

    try {
      if (refreshToken != null) {
        remoteUserQueryService.logout(refreshToken).join();
        remoteLogoutSucceeded = true;
      } else {
        remoteLogoutSucceeded = true;
      }
    } catch (Throwable throwable) {
      remoteFailure = unwrap(throwable);
    } finally {
      authTokensService.clearAuthTokens();
      userService.clearUser();
    }

    if (remoteFailure == null) {
      return new LogoutResult(true, remoteLogoutSucceeded, LogoutReason.SUCCESS, "Signed out successfully.");
    }

    LogoutReason reason = classifyLogoutFailure(remoteFailure);
    String message = switch (reason) {
      case SESSION_EXPIRED -> "Session expired. Signed out locally. Please log in again.";
      case NETWORK_FAILURE -> "Could not reach server. Signed out locally.";
      default -> "Signed out locally. Server logout could not be confirmed.";
    };

    return new LogoutResult(true, false, reason, message);
  }

  public User getCurrentUser() {
    return userService.getCurrentUser();
  }

  public boolean isLoggedIn() {
    return !userService.isGuest();
  }

  public void refreshSession() {
    String refreshToken = authTokensService.getAuthTokens()
        .map(AuthTokens::getRefreshToken)
        .orElseThrow(() -> new IllegalStateException("No refresh token available"));
    TokensResponse refreshResponse = remoteUserQueryService.refreshTokens(refreshToken).join();
    LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(refreshResponse.getExpiresIn()));
    AuthTokens newTokens = new AuthTokens(refreshResponse.getAccessToken(), refreshResponse.getRefreshToken(),
        expiresAt);
    authTokensService.saveAuthTokens(newTokens);
  }

  public void refreshSessionIfNeeded() {
    if (authTokensService.isAccessTokenExpired())
      refreshSession();
  }

  private LogoutReason classifyLogoutFailure(Throwable throwable) {
    if (throwable instanceof UnauthorizedException) {
      return LogoutReason.SESSION_EXPIRED;
    }

    String message = throwable.getMessage();
    if (message != null) {
      String normalized = message.toLowerCase();
      if (normalized.contains("token expired")
          || normalized.contains("refresh token expired")
          || normalized.contains("unauthorized")) {
        return LogoutReason.SESSION_EXPIRED;
      }
    }

    if (throwable instanceof ConnectException
        || throwable instanceof SocketTimeoutException
        || throwable instanceof SocketException) {
      return LogoutReason.NETWORK_FAILURE;
    }

    return LogoutReason.UNKNOWN_FAILURE;
  }

  private Throwable unwrap(Throwable throwable) {
    Throwable current = throwable;
    while (current instanceof CompletionException
        || current instanceof ExecutionException
        || current instanceof InvocationTargetException) {
      Throwable cause = current.getCause();
      if (cause == null || cause == current) {
        break;
      }
      current = cause;
    }
    return current;
  }
}
