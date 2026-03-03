package com.ks.bayyinah.infra.remote.client;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.ks.bayyinah.infra.exception.UnauthorizedException;
import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.hybrid.model.MainConfig;
import com.ks.bayyinah.infra.hybrid.query.TokenManager;
import com.ks.bayyinah.infra.remote.routing.ApiRoute;
import com.ks.bayyinah.infra.remote.routing.RouteResolver;

import tools.jackson.databind.ObjectMapper;

public class ApiClient {
  private final HttpClient httpClient;
  private final MainConfig mainConfig;
  private final RouteResolver routeResolver;
  private final TokenManager tokenManager;
  private final ObjectMapper objectMapper;

  public ApiClient(MainConfig mainConfig, TokenManager tokenManager) {
    this.mainConfig = mainConfig;
    this.routeResolver = new RouteResolver(mainConfig);
    this.objectMapper = new ObjectMapper();
    this.tokenManager = tokenManager;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(mainConfig.getApi().getConnectTimeoutSeconds()))
        .build();

    this.tokenManager.setTokenRefreshCallback(this::performTokenRefresh);
  }

  // GET (with auth)
  public <T> CompletableFuture<T> get(ApiRoute route, Class<T> responseType, Object... pathParams) {
    return tokenManager.getAccessToken().thenCompose(token -> {
      String url = routeResolver.resolve(route, pathParams);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + token)
          .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
          .GET()
          .build();

      return executeRequest(request, responseType);
    });
  }

  // GET (without auth)
  public <T> CompletableFuture<T> getPublic(ApiRoute route, Class<T> responseType, Object... pathParams) {
    String url = routeResolver.resolve(route, pathParams);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
        .GET()
        .build();

    return executeRequest(request, responseType);
  }

  // POST (with auth)
  public <T> CompletableFuture<T> post(ApiRoute route, T body, Class<T> responseType, Object... pathParams) {
    return tokenManager.getAccessToken().thenCompose(token -> {
      String url = routeResolver.resolve(route, pathParams);
      String jsonBody = objectMapper.writeValueAsString(body);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + token)
          .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

      return executeRequest(request, responseType);
    });
  }

  // POST (without auth)
  public <T> CompletableFuture<T> postPublic(ApiRoute route, T body, Class<T> responseType, Object... pathParams) {
    String url = routeResolver.resolve(route, pathParams);
    String jsonBody = objectMapper.writeValueAsString(body);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();

    return executeRequest(request, responseType);
  }

  // PUT (with auth)
  public <T> CompletableFuture<T> put(ApiRoute route, T body, Class<T> responseType, Object... pathParams) {
    return tokenManager.getAccessToken().thenCompose(token -> {
      String url = routeResolver.resolve(route, pathParams);
      String jsonBody = objectMapper.writeValueAsString(body);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + token)
          .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
          .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

      return executeRequest(request, responseType);
    });
  }

  // PUT (without auth)
  public <T> CompletableFuture<T> putPublic(ApiRoute route, T body, Class<T> responseType, Object... pathParams) {
    String url = routeResolver.resolve(route, pathParams);
    String jsonBody = objectMapper.writeValueAsString(body);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();

    return executeRequest(request, responseType);
  }

  // DELETE (with auth)
  public <T> CompletableFuture<T> delete(ApiRoute route, Class<T> responseType, Object... pathParams) {
    return tokenManager.getAccessToken().thenCompose(token -> {
      String url = routeResolver.resolve(route, pathParams);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + token)
          .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
          .DELETE()
          .build();

      return executeRequest(request, responseType);
    });
  }

  // DELETE (without auth)
  public <T> CompletableFuture<T> deletePublic(ApiRoute route, Class<T> responseType, Object... pathParams) {
    String url = routeResolver.resolve(route, pathParams);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
        .DELETE()
        .build();

    return executeRequest(request, responseType);
  }

  private <T> CompletableFuture<T> executeRequest(HttpRequest request, Class<T> responseType) {
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> handleResponse(response, responseType));
  }

  private CompletableFuture<AuthTokens> performTokenRefresh(String refreshToken) {
    // TODO: implement the real logic here
    return CompletableFuture.completedFuture(new AuthTokens());
  }

  private <T> T handleResponse(HttpResponse<String> response, Class<T> responseType) {
    int statusCode = response.statusCode();
    String body = response.body();

    if (statusCode >= 200 && statusCode < 300) {
      try {
        if (responseType == Void.class || body == null || body.isEmpty()) {
          return null;
        }
        return objectMapper.readValue(body, responseType);
      } catch (Exception e) {
        throw new ApiException("Failed to parse response: " + e.getMessage(), e);
      }
    }

    if (statusCode == 401) {
      throw new ApiException("Unauthorized: Access token may have expired");
    }

    if (statusCode == 403) {
      throw new UnauthorizedException("Forbidden: You do not have permission to access this resource");
    }

    if (statusCode == 404) {
      throw new ApiException("Not Found: The requested resource does not exist");
    }

    if (statusCode >= 500) {
      throw new ApiException("Server error: " + statusCode);
    }

    // Try to parse error message from body
    try {
      ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
      throw new ApiException(error.getMessage());
    } catch (Exception e) {
      throw new ApiException("Request failed with status: " + statusCode);
    }
  }

  public static class ApiException extends RuntimeException {
    public ApiException(String message) {
      super(message);
    }

    public ApiException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private static class ErrorResponse {
    private String message;

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

  private static class RefreshTokenRequest {
    private String refreshToken;

    public RefreshTokenRequest(String refreshToken) {
      this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }
  }
}
