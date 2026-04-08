package com.ks.bayyinah.ui.theme;

public enum LightPaletteVariant {
  EMERALD_MODERN("light-variant-emerald-modern"),
  OLIVE_WARM("light-variant-olive-warm"),
  FOREST_DEEP("light-variant-forest-deep");

  public static final LightPaletteVariant DEFAULT = EMERALD_MODERN;

  private final String styleClass;

  LightPaletteVariant(String styleClass) {
    this.styleClass = styleClass;
  }

  public String getStyleClass() {
    return styleClass;
  }
}