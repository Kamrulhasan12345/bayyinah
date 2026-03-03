package com.ks.bayyinah.infra.hybrid.query;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import com.ks.bayyinah.infra.exception.UnauthorizedException;
import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.hybrid.service.AuthTokensService;

import lombok.Setter;

public class TokenManager {
  private final AuthTokensService authTokensService;
  private final ReentrantLock refreshLock = new ReentrantLock();

  @Setter
  private TokenRefreshCallback tokenRefreshCallback;

  public TokenManager(AuthTokensService authTokensService) {
    this.authTokensService = authTokensService;
  }

  public CompletableFuture<String> getAccessToken() {
    Optional<AuthTokens> tokensOpt = authTokensService.getAuthTokens();

    if (tokensOpt.isEmpty()) {
      return CompletableFuture.failedFuture(new UnauthorizedException("No auth tokens found"));
    }

    AuthTokens tokens = tokensOpt.get();

    if (!tokens.isExpired()) {
      return CompletableFuture.completedFuture(tokens.getAccessToken());
    }

    return refreshAccessToken(tokens);
  }

  private CompletableFuture<String> refreshAccessToken(AuthTokens tokens) {
    refreshLock.lock();

    try {
      Optional<AuthTokens> tokensOpt = authTokensService.getAuthTokens();
      if (tokensOpt.isPresent() && !tokensOpt.get().isExpired()) {
        return CompletableFuture.completedFuture(tokensOpt.get().getAccessToken());
      }

      if (tokens.isRefreshTokenExpired()) {
        return CompletableFuture.failedFuture(new UnauthorizedException("Refresh token expired, please log in again"));
      }

      if (tokenRefreshCallback == null) {
        return CompletableFuture.failedFuture(new IllegalStateException("Token refresh callback not set"));
      }

      CompletableFuture<AuthTokens> refreshFuture = tokenRefreshCallback.refreshToken(tokens.getRefreshToken());
      return refreshFuture.thenApply(newTokens -> {
        authTokensService.saveAuthTokens(newTokens);
        refreshLock.unlock();
        return newTokens.getAccessToken();
      }).exceptionally(ex -> {
        throw new UnauthorizedException("Failed to refresh access token", ex);
      });

    } catch (Exception e) {
      refreshLock.unlock();
      return CompletableFuture.failedFuture(new UnauthorizedException("Failed to refresh access token", e));
    }
  }

  public interface TokenRefreshCallback {
    CompletableFuture<AuthTokens> refreshToken(String refreshToken);
  }
}
