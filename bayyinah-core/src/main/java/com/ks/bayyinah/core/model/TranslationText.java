package com.ks.bayyinah.core.model;

public class TranslationText {
  private int id;
  private int verse_id;
  private int translation_id;
  private String text;

  public TranslationText(int id, int verse_id, int translation_id, String text) {
    this.id = id;
    this.verse_id = verse_id;
    this.translation_id = translation_id;
    this.text = text;
  }

  public int getId() {
    return this.id;
  }

  public int getVerseId() {
    return this.verse_id;
  }

  public int getTranslationId() {
    return this.translation_id;
  }

  public String getText() {
    return this.text;
  }
}
