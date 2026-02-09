package com.ks.bayyinah.core.models;

public class TranslationText {
  private int verse_id;
  private int translation_id;
  private String text;

  public TranslationText(int verse_id, int translation_id, String text) {
    this.verse_id = verse_id;
    this.translation_id = translation_id;
    this.text = text;
  }

  public String getText() {
    return this.text;
  }

  public int getVerseId() {
    return this.verse_id;
  }

  public int getTranslationId() {
    return this.translation_id;
  }
}
