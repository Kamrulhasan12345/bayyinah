package com.ks.bayyinah.infra.remote.routing;

public enum ApiRoute {

  // Auth routes (user API)
  AUTH_LOGIN("/api/auth/login"),
  AUTH_REGISTER("/api/auth/register"),
  AUTH_REFRESH("/api/auth/refresh"),
  AUTH_LOGOUT("/api/auth/logout"),

  /* FIXME: ALL EXCEPT AUTH INACTIVE FOR NOW */
  // User routes (user API)
  USER_PROFILE("/api/users/me"),
  USER_PREFERENCES("/api/users/preferences"),

  // Bookmark routes (user API)
  BOOKMARKS("/api/bookmarks"),
  BOOKMARKS_BY_ID("/api/bookmarks/{id}"),
  BOOKMARKS_SYNC("/api/bookmarks/sync"),

  // Note routes (user API)
  NOTES("/api/notes"),
  NOTES_BY_ID("/api/notes/{id}"),
  NOTES_SYNC("/api/notes/sync"),
  NOTES_SEARCH("/api/notes/search"),

  // Progress routes (user API)
  PROGRESS("/api/progress"),
  PROGRESS_STATS("/api/progress/stats"),
  PROGRESS_SYNC("/api/progress/sync"),

  // External Quran API routes (quran API)
  QURAN_CHAPTERS("/chapters"),
  QURAN_CHAPTER_BY_ID("/chapters/{id}"),
  QURAN_VERSES("/verses"),
  QURAN_TRANSLATIONS("/translations");

  private final String path;

  ApiRoute(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  public String format(Object... args) {
    String formattedPath = path;
    for (Object arg : args) {
      formattedPath = formattedPath.replaceFirst("\\{[^}]+\\}", arg.toString());
    }
    return formattedPath;
  }
}
