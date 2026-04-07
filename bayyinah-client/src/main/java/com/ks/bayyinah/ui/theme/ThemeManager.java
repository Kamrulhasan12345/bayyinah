package com.ks.bayyinah.ui.theme;

import java.net.URL;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

  private static final ThemeManager INSTANCE = new ThemeManager();
  private static final String ROOT_THEME_CLASS = "theme-root";
  private static final String THEME_STYLESHEET_RESOURCE = "/com/ks/bayyinah/css/theme.css";

  private Scene activeScene;
  private ThemeMode currentTheme = ThemeMode.LIGHT;
  private LightPaletteVariant activeLightVariant = LightPaletteVariant.DEFAULT;

  private ThemeManager() {
  }

  public static ThemeManager getInstance() {
    return INSTANCE;
  }

  public synchronized ThemeMode getCurrentTheme() {
    return currentTheme;
  }

  public synchronized LightPaletteVariant getActiveLightVariant() {
    return activeLightVariant;
  }

  public synchronized void setActiveLightVariant(LightPaletteVariant variant) {
    if (variant == null) {
      return;
    }

    this.activeLightVariant = variant;
    if (activeScene != null && currentTheme == ThemeMode.LIGHT) {
      applyTheme(activeScene, currentTheme);
    }
  }

  public synchronized void applyTheme(Scene scene, String rawTheme) {
    applyTheme(scene, ThemeMode.fromPreference(rawTheme));
  }

  public synchronized void applyTheme(Scene scene, ThemeMode mode) {
    if (scene == null || scene.getRoot() == null) {
      return;
    }

    ensureThemeStylesheet(scene);
    Parent root = scene.getRoot();
    ObservableList<String> rootStyleClasses = root.getStyleClass();

    if (!rootStyleClasses.contains(ROOT_THEME_CLASS)) {
      rootStyleClasses.add(ROOT_THEME_CLASS);
    }

    rootStyleClasses.removeAll(
        ThemeMode.LIGHT.getStyleClass(),
        ThemeMode.DARK.getStyleClass(),
        ThemeMode.SEPIA.getStyleClass(),
        LightPaletteVariant.EMERALD_MODERN.getStyleClass(),
        LightPaletteVariant.OLIVE_WARM.getStyleClass(),
        LightPaletteVariant.FOREST_DEEP.getStyleClass());

    ThemeMode effectiveMode = mode == null ? ThemeMode.LIGHT : mode;
    rootStyleClasses.add(effectiveMode.getStyleClass());

    if (effectiveMode == ThemeMode.LIGHT) {
      rootStyleClasses.add(activeLightVariant.getStyleClass());
    }

    this.activeScene = scene;
    this.currentTheme = effectiveMode;
  }

  private void ensureThemeStylesheet(Scene scene) {
    URL stylesheetUrl = ThemeManager.class.getResource(THEME_STYLESHEET_RESOURCE);
    if (stylesheetUrl == null) {
      return;
    }

    String stylesheet = stylesheetUrl.toExternalForm();
    if (!scene.getStylesheets().contains(stylesheet)) {
      scene.getStylesheets().add(stylesheet);
    }
  }
}