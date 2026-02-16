package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.model.*;

public class VerseView {
  private Verse verse;
  private TranslationText translation_text;

  public VerseView(Verse verse, TranslationText translation_text) {
    this.verse = verse;
    this.translation_text = translation_text;
  }

  public Verse getVerse() {
    return verse;
  }

  public TranslationText getTranslationText() {
    return translation_text;
  }

  public String getArabicText() {
    return verse != null ? verse.getText() : null;
  }

  public String getTranslatedText() {
    return translation_text != null ? translation_text.getText() : null;
  }

  public String getVerseKey() {
    return verse != null ? verse.getVerseKey() : null;
  }
};
