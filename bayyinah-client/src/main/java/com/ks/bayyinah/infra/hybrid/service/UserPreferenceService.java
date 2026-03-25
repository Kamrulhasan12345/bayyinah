package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.hybrid.model.UserPreference;
import com.ks.bayyinah.infra.local.repository.user.UserPreferenceRepository;
import java.util.Map;

public class UserPreferenceService {
  // This service will manage user preferences. It will interact with the local
  // database to save and retrieve user preferences, and also sync with the
  // server when needed.

  // For simplicity, let's assume user preferences are represented by a key-value
  // pair.

  // We will need methods to:
  // - Set a user preference
  // - Get a user preference
  // - Sync user preferences with the server

  // The implementation details will depend on the specific requirements and data
  // models of the application.
  private final UserPreferenceRepository repository;
  private final SyncQueueService syncQueueService;

  public UserPreferenceService(UserPreferenceRepository repository) {
    this(repository, null);
  }

  public UserPreferenceService(UserPreferenceRepository repository, SyncQueueService syncQueueService) {
    this.repository = repository;
    this.syncQueueService = syncQueueService;
  }

  public void setPreference(String key, String value) {
    repository.insertOrUpdate(key, value);
    enqueuePreferenceUpsert(key, value);
  }

  public void setPreferences(Map<String, String> preferences) {
    if (preferences == null || preferences.isEmpty()) {
      return;
    }

    for (Map.Entry<String, String> entry : preferences.entrySet()) {
      setPreference(entry.getKey(), entry.getValue());
    }
  }

  public UserPreference getPreference(String key) {
    return repository.get(key);
  }

  public Integer getDefaultTranslation() {
    UserPreference pref = getPreference("default_translation");
    if (pref != null && pref.getValue() != null) {
      try {
        return Integer.parseInt(pref.getValue().trim());
      } catch (NumberFormatException e) {
        // Log the error and return null or a default value
        e.printStackTrace();
      }
    }
    return 20; // or return a default translation ID
  }

  private void enqueuePreferenceUpsert(String key, String value) {
    if (syncQueueService == null || key == null || key.isBlank()) {
      return;
    }

    long recordId = Integer.toUnsignedLong(key.hashCode());
    String payload = "{" +
        "\"key\":\"" + escapeJson(key) + "\"," +
        "\"value\":\"" + escapeJson(value == null ? "" : value) + "\"" +
        "}";
    syncQueueService.enqueueUpsert("user_preferences", recordId, payload);
  }

  private String escapeJson(String input) {
    return input
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }
}
