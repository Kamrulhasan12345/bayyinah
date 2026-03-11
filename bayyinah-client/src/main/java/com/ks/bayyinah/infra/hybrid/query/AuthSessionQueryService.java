package com.ks.bayyinah.infra.hybrid.query;

import java.time.Duration;
import java.time.LocalDateTime;

import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.hybrid.model.User;
import com.ks.bayyinah.infra.hybrid.service.AuthTokensService;
import com.ks.bayyinah.infra.hybrid.service.UserService;
import com.ks.bayyinah.infra.remote.dto.auth.TokensResponse;
import com.ks.bayyinah.infra.remote.query.RemoteUserQueryService;

import lombok.*;

@AllArgsConstructor
public class AuthSessionQueryService {
  private final AuthTokensService authTokensService;
  private final UserService userService;
  private final RemoteUserQueryService remoteUserQueryService;

  public User ensureGuestSession() {
    if (authTokensService.getAuthTokens().isEmpty()) {
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
    }).join();
  }

  public void register(String username, String email, String password, String firstName, String lastName) {
    remoteUserQueryService.register(username, email, password, firstName, lastName).thenAccept(regResponse -> {
      login(username, password);
    }).join();
  }

  public void logout() {
    String refreshToken = authTokensService.getAuthTokens()
        .map(AuthTokens::getRefreshToken)
        .orElseThrow(() -> new IllegalStateException("No refresh token available"));
    remoteUserQueryService.logout(refreshToken).join();
    authTokensService.clearAuthTokens();
    userService.clearUser();
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
}
