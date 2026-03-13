package com.ks.bayyinah.infra.remote.client;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import com.ks.bayyinah.infra.exception.UnauthorizedException;
import com.ks.bayyinah.infra.hybrid.model.AuthTokens;
import com.ks.bayyinah.infra.hybrid.model.MainConfig;
import com.ks.bayyinah.infra.hybrid.query.TokenManager;
import com.ks.bayyinah.infra.remote.dto.auth.RefreshTokenRequest;
import com.ks.bayyinah.infra.remote.dto.auth.TokensResponse;
import com.ks.bayyinah.infra.remote.routing.ApiRoute;
import com.ks.bayyinah.infra.remote.routing.RouteResolver;
import com.ks.bayyinah.error.GlobalExceptionHandler;

import tools.jackson.databind.ObjectMapper;

public class ApiClient {
  private final HttpClient httpClient;
  private final MainConfig mainConfig;
  private final RouteResolver routeResolver;
  private final TokenManager tokenManager;
  private final ObjectMapper objectMapper;
  private final GlobalExceptionHandler exceptionHandler;

  public ApiClient(MainConfig mainConfig, TokenManager tokenManager) {
    this.exceptionHandler = new GlobalExceptionHandler();
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
  public <Req, Res> CompletableFuture<Res> get(ApiRoute route, Class<Res> responseType, Object... pathParams) {
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
    }).exceptionally(error -> {
                // Handle error globally
                exceptionHandler.handleException(error, "GET " + route.getPath());
                return null;
            });
  }

  // GET (without auth)
  public <Req, Res> CompletableFuture<Res> getPublic(ApiRoute route, Class<Res> responseType, Object... pathParams) {
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
  public <Req, Res> CompletableFuture<Res> post(ApiRoute route, Req body, Class<Res> responseType,
      Object... pathParams) {
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
    }).exceptionally(error -> {
                // Handle error globally
                exceptionHandler.handleException(error, "POST " + route.getPath());
                return null;
            });
  }

  // POST (without auth)
  public <Req, Res> CompletableFuture<Res> postPublic(ApiRoute route, Req body, Class<Res> responseType,
      Object... pathParams) {
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
  public <Req, Res> CompletableFuture<Res> put(ApiRoute route, Req body, Class<Res> responseType,
      Object... pathParams) {
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
    }).exceptionally(error -> {
                // Handle error globally
                exceptionHandler.handleException(error, "PUT " + route.getPath());
                return null;
            });
  }

  // PUT (without auth)
  public <Req, Res> CompletableFuture<Res> putPublic(ApiRoute route, Req body, Class<Res> responseType,
      Object... pathParams) {
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
  public <Req, Res> CompletableFuture<Res> delete(ApiRoute route, Class<Res> responseType, Object... pathParams) {
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
    }).exceptionally(error -> {
                // Handle error globally
                exceptionHandler.handleException(error, "DELETE " + route.getPath());
                return null;
            });
  }

  // DELETE (without auth)
  public <Req, Res> CompletableFuture<Res> deletePublic(ApiRoute route, Class<Res> responseType, Object... pathParams) {
    String url = routeResolver.resolve(route, pathParams);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
        .DELETE()
        .build();

    return executeRequest(request, responseType);
  }

  private <Req, Res> CompletableFuture<Res> executeRequest(HttpRequest request, Class<Res> responseType) {
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> handleResponse(response, responseType));
  }

  private CompletableFuture<AuthTokens> performTokenRefresh(String refreshToken) {
    // TODO: implement the real logic here
    String url = routeResolver.resolve(ApiRoute.AUTH_REFRESH);

    try {
      RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
      String jsonBody = objectMapper.writeValueAsString(refreshRequest);

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .timeout(Duration.ofSeconds(mainConfig.getApi().getRequestTimeoutSeconds()))
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

      return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
          .thenApply(response -> {
            if (response.statusCode() != 200) {
              throw new UnauthorizedException("Failed to refresh token: " + response.statusCode());
            }

            try {
              TokensResponse refreshResponse = objectMapper.readValue(response.body(), TokensResponse.class);

              LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(refreshResponse.getExpiresIn()));
              AuthTokens newTokens = new AuthTokens(refreshResponse.getAccessToken(), refreshResponse.getRefreshToken(),
                  expiresAt);

              return newTokens;
            } catch (Exception e) {
              throw new ApiException("Failed to parse token refresh response: " + e.getMessage(), e);
            }
          });
    } catch (Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  private <Req, Res> Res handleResponse(HttpResponse<String> response, Class<Res> responseType) {
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
      throw new ApiException("Unauthorized: Wrong login credientials");
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
}
