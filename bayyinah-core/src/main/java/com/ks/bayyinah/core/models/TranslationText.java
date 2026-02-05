package com.ks.bayyinah.core.models;

public class TranslationText {
  private int id;
  private int verse_id;
  private int translation_id;
  private String text;

  public TranslationText(int id, int verse_id, int translation_id, String text) {
    this.id = id;
    this.verse_id = id;
    this.translation_id = translation_id;
    this.text = text;
  }
}
