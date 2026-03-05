package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainConfig {
  private QuranConfig quran;
  private UserConfig user;
  private ApiConfig api;
  private String apiUrl;

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
