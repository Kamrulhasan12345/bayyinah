package com.ks.bayyinah.infra.remote.query;

import java.util.concurrent.CompletableFuture;

import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.remote.client.ApiClient;
import com.ks.bayyinah.infra.remote.dto.auth.*;
import com.ks.bayyinah.infra.remote.routing.ApiRoute;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RemoteUserQueryService {
  private final ApiClient apiClient;

  public CompletableFuture<LoginResponse> login(String username, String password) {
    LoginRequest request = new LoginRequest(username, password);
    return apiClient.postPublic(ApiRoute.AUTH_LOGIN, request, LoginResponse.class);
  }

  public CompletableFuture<RegistrationResponse> register(
      String username,
      String email,
      String password,
      String firstName,
      String lastName) {
    RegistrationRequest request = new RegistrationRequest(
        username, email, password, firstName, lastName);
    return apiClient.postPublic(ApiRoute.AUTH_REGISTER, request, RegistrationResponse.class);
  }

  public CompletableFuture<TokensResponse> refreshTokens(String refreshToken) {
    RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
    return apiClient.post(ApiRoute.AUTH_REFRESH, request, TokensResponse.class);
  }

  public CompletableFuture<Void> logout() {
    return apiClient.post(ApiRoute.AUTH_LOGOUT, null, Void.class);
  }
}
