package com.ks.bayyinah.ui.theme;

public enum ThemeMode {
  LIGHT("light", "theme-light"),
  DARK("dark", "theme-dark"),
  SEPIA("sepia", "theme-sepia");

  private final String preferenceValue;
  private final String styleClass;

  ThemeMode(String preferenceValue, String styleClass) {
    this.preferenceValue = preferenceValue;
    this.styleClass = styleClass;
  }

  public String getPreferenceValue() {
    return preferenceValue;
  }

  public String getStyleClass() {
    return styleClass;
  }

  public static ThemeMode fromPreference(String rawValue) {
    if (rawValue == null) {
      return LIGHT;
    }

    String normalized = rawValue.trim().toLowerCase();
    for (ThemeMode mode : values()) {
      if (mode.preferenceValue.equals(normalized)) {
        return mode;
      }
    }

    return LIGHT;
  }
}