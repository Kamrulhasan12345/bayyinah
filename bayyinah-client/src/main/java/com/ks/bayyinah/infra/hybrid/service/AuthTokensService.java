package com.ks.bayyinah.infra.hybrid.service;

import java.util.Optional;

import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.local.repository.user.AuthTokensRepository;

public class AuthTokensService {

  private final AuthTokensRepository repository;

  public AuthTokensService(AuthTokensRepository repository) {
    this.repository = repository;
  }

  public Optional<AuthTokens> getAuthTokens() {
    return repository.get();
  }

  public void saveAuthTokens(AuthTokens tokens) {
    repository.insertOrUpdate(tokens);
  }

  public void clearAuthTokens() {
    repository.clear();
  }

  public boolean isAccessTokenExpired() {
    return repository.isExpired();
  }

  public void updateAccessToken(String newAccessToken, int expiresInSeconds) {
    Optional<AuthTokens> tokensOpt = repository.get();
    if (tokensOpt.isPresent()) {
      AuthTokens tokens = tokensOpt.get();
      tokens.setAccessToken(newAccessToken);
      tokens.setExpiresAt(tokens.getIssuedAt().plusSeconds(expiresInSeconds));
      repository.insertOrUpdate(tokens);
    }
  }

  public void updateRefreshToken(String newRefreshToken) {
    Optional<AuthTokens> tokensOpt = repository.get();
    if (tokensOpt.isPresent()) {
      AuthTokens tokens = tokensOpt.get();
      tokens.setRefreshToken(newRefreshToken);
      repository.insertOrUpdate(tokens);
    }
  }
}
