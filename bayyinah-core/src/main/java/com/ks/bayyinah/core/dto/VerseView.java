package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.models.Verse;
import com.ks.bayyinah.core.models.Translation;
import com.ks.bayyinah.core.models.TranslationText;

public class VerseView {
  private Verse arabic;
  private TranslationText translation;

  public VerseView(Verse arabic, TranslationText translation) {
    this.arabic = arabic;
    this.translation = translation;
  }

  public Verse getVerse() {
    return arabic;
  }

  public TranslationText getTranslation() {
    return translation;
  }

  public String getVerseKey() {
    return arabic.getVerseKey();
  }

  public String getArabicText() {
    return arabic.getText();
  }

  public TranslationText getTranslationText() {
    return translation;
  }

  public void setArabic(Verse arabic) {
    this.arabic = arabic;
  }

  public void setTranslation(TranslationText translation) {
    this.translation = translation;
  }
};
