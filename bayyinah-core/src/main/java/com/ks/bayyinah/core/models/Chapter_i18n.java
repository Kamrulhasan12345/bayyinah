package com.ks.bayyinah.core.models;

public class Chapter_i18n {
  private int id;
  private int chapter_id;
  private String lang_code;
  private String translated_name;
  private String short_text;
  private String full_text;

  public Chapter_i18n(int id, int chapter_id, String lang_code, String translated_name, String short_text,
      String full_text) {
    this.id = id;
    this.chapter_id = chapter_id;
    this.lang_code = lang_code;
    this.translated_name = translated_name;
    this.short_text = short_text;
    this.full_text = full_text;
  }
}
