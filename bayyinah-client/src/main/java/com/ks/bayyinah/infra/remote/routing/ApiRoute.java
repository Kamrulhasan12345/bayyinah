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
  USER_PREFERENCES("/api/users/me/preferences"),

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
  PROGRESS_BY_ID("/api/progress/{id}"),
  PROGRESS_STATS("/api/progress/stats"),
  PROGRESS_SYNC("/api/progress/sync"),

  // HALAQAH routes (user API)
  HALAQAH_ROOM("/api/halaqah/{code}"),
  HALAQAH_CREATE_ROOM("/api/halaqah/create"),
  HALAQAH_JOIN_ROOM("/api/halaqah/join"),
  HALAQAH_LEAVE_ROOM("/api/halaqah/leave"),

  // AI routes (AI API)
  AI_RECOMMEND("/v2/recommend"),
  AI_RECOMMEND_REFLECT("/v2/recommend-with-reflection"),

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
