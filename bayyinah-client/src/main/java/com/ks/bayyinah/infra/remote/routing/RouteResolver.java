package com.ks.bayyinah.infra.remote.routing;

import com.ks.bayyinah.infra.hybrid.model.MainConfig;

public class RouteResolver {

  private final MainConfig config;

  public RouteResolver(MainConfig config) {
    this.config = config;
  }

  /**
   * Resolve full URL based on route type
   */
  public String resolve(ApiRoute route) {
    String baseUrl = getBaseUrl(route);
    return baseUrl + route.getPath();
  }

  /**
   * Resolve full URL with path parameters
   */
  public String resolve(ApiRoute route, Object... params) {
    String baseUrl = getBaseUrl(route);
    return baseUrl + route.format(params);
  }

  /**
   * Determine which base URL to use based on route
   */
  private String getBaseUrl(ApiRoute route) {
    String routePath = route.getPath();

    // Quran API routes (external)
    if (isQuranRoute(routePath)) {
      return config.getQuranApiUrl();
    }

    // User API routes (can be separate microservice)
    if (isUserRoute(routePath)) {
      return config.getUserApiUrl();
    }

    // Fallback to main API
    return config.getMainApiUrl();
  }

  /**
   * Check if route is for external Quran API
   */
  private boolean isQuranRoute(String path) {
    return path.startsWith("/chapters") ||
        path.startsWith("/verses") ||
        path.startsWith("/translations");
  }

  /**
   * Check if route is for user/auth services
   */
  private boolean isUserRoute(String path) {
    return path.startsWith("/api/auth") ||
        path.startsWith("/api/users") ||
        path.startsWith("/api/bookmarks") ||
        path.startsWith("/api/notes") ||
        path.startsWith("/api/progress");
  }
}
