package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.model.Translation;

public class TranslationView {
  private int id;
  private Translation translation;

  public TranslationView(int id, Translation translation) {
    this.id = id;
    this.translation = translation;
  }

  public int getId() {
    return id;
  }

  public Translation getTranslation() {
    return translation;
  }
}
