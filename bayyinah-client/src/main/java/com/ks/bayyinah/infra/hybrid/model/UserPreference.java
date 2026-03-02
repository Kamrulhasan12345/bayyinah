package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserPreference {

  private Long id;
  private String key;
  private String value;
  private LocalDateTime updatedAt;
  private boolean synced;

  // Constructors
  public UserPreference(String key, String value) {
    this.key = key;
    this.value = value;
    this.updatedAt = LocalDateTime.now();
    this.synced = false;
  }

  // Getters and Setters
  public void setValue(String value) {
    this.value = value;
    this.updatedAt = LocalDateTime.now();
    this.synced = false;
  }

  // Type conversion helpers
  public int getValueAsInt(int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public boolean getValueAsBoolean(boolean defaultValue) {
    if (value == null)
      return defaultValue;
    return "true".equalsIgnoreCase(value);
  }
}
