package com.ks.bayyinah.infra.hybrid.model;

import java.nio.file.Path;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainConfig {
  private QuranConfig quran;
  private UserConfig user;
  private ApiConfig api;
  private AudioConfig audio;
  private String apiUrl;
  private String aiApiUrl;

  private static final int DEFAULT_ACTIVE_RECITER_ID = 2;

  public void loadFallbacksAsNeeded() {
    this.apiUrl = (this.apiUrl != null) ? this.apiUrl : "http://localhost:8080";
    this.quran = (this.quran != null) ? this.quran
        : new QuranConfig(
            System.getProperty("user.home") + "/.bayyinah/quran.db",
            this.apiUrl);
    this.user = (this.user != null) ? this.user
        : new UserConfig(
            System.getProperty("user.home") + "/.bayyinah/user.db",
            this.apiUrl);
    this.api = (this.api != null) ? this.api : new ApiConfig(10, 30, 3, 5000);
    String defaultAudioRootPath = resolveDefaultAudioRootPath();
    this.audio = (this.audio != null) ? this.audio : new AudioConfig(defaultAudioRootPath, DEFAULT_ACTIVE_RECITER_ID);
    if (this.audio.getAudioRootPath() == null || this.audio.getAudioRootPath().isBlank()) {
      this.audio.setAudioRootPath(defaultAudioRootPath);
    }
    if (this.audio.getActiveReciterId() == null || this.audio.getActiveReciterId() <= 0) {
      this.audio.setActiveReciterId(DEFAULT_ACTIVE_RECITER_ID);
    }
    this.aiApiUrl = (this.aiApiUrl != null) ? this.aiApiUrl : "https://kamrulhasan12345-bayyinah-ai.hf.space";
  }

  private String resolveDefaultAudioRootPath() {
    try {
      if (quran != null && quran.getDatabasePath() != null && !quran.getDatabasePath().isBlank()) {
        Path quranPath = Path.of(quran.getDatabasePath());
        Path parent = quranPath.getParent();
        if (parent != null) {
          return parent.resolve("audio").toString();
        }
      }
    } catch (Exception ignored) {
      // Fall back to home directory based default if quran path is invalid.
    }

    return Path.of(System.getProperty("user.home"), ".bayyinah", "audio").toString();
  }

  public String getQuranApiUrl() {
    return quran.getApiUrl();
  }

  public String getUserApiUrl() {
    return user.getApiUrl();
  }

  public String getMainApiUrl() {
    return apiUrl;
  }
}
