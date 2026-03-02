package com.ks.bayyinah.infra.hybrid.query;

import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.hybrid.model.User;
import com.ks.bayyinah.infra.hybrid.service.AuthTokensService;
import com.ks.bayyinah.infra.hybrid.service.UserService;
import com.ks.bayyinah.infra.remote.query.RemoteUserQueryService;

import lombok.*;

@AllArgsConstructor
public class AuthSessionQueryService {
  private final AuthTokensService authTokensService;
  private final UserService userService;
  private final RemoteUserQueryService remoteUserQueryService;

  public void ensureGuestSession() {
    if (authTokensService.getAuthTokens().isEmpty()) {
      userService.createGuestUser();
    }
  }

  public void login() {

  }

  public void signup() {

  }

  public void logout() {
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
    AuthTokens newTokens = remoteUserQueryService.refreshTokens();
    authTokensService.saveAuthTokens(newTokens);
  }

  public void refreshSessionIfNeeded() {
    if (authTokensService.isAccessTokenExpired())
      refreshSession();
  }
}
