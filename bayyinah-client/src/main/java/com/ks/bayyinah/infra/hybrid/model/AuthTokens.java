package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
public class AuthTokens {

  private Long id;
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private LocalDateTime expiresAt;
  private LocalDateTime issuedAt;
  private LocalDateTime updatedAt;
  private boolean encrypted;

  public AuthTokens() {
    this.tokenType = "Bearer";
    this.encrypted = false;
  }

  public AuthTokens(String accessToken, String refreshToken, int expiresInSeconds) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenType = "Bearer";
    this.expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);
    this.issuedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.encrypted = false;
  }

  public AuthTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenType = "Bearer";
    this.expiresAt = expiresAt;
    this.issuedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.encrypted = false;
  }

  public boolean isExpired() {
    if (expiresAt == null)
      return true;
    return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
  }

  public boolean isRefreshTokenExpired() {
    // Assuming refresh tokens expire after 30 days
    if (issuedAt == null)
      return true;
    return LocalDateTime.now().isAfter(issuedAt.plusDays(7));
  }

  public String getAuthorizationHeader() {
    if (accessToken == null)
      return null;
    return tokenType + " " + accessToken;
  }

  public long getSecondsUntilExpiry() {
    if (expiresAt == null)
      return 0;
    return java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
  }
}
