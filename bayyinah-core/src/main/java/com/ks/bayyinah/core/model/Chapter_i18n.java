package com.ks.bayyinah.core.model;

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

  public int getId() {
    return id;
  }

  public int getChapterId() {
    return chapter_id;
  }

  public String getLangCode() {
    return lang_code;
  }

  public String getTranslatedName() {
    return translated_name;
  }

  public String getShortText() {
    return short_text;
  }

  public String getFullText() {
    return full_text;
  }

  @Override
  public String toString() {
    return "Chapter_i18n{id=" + id + ", chapter_id=" + chapter_id + ", lang_code='" + lang_code + "', translated_name='"
        + translated_name + "', short_text='" + short_text + "', full_text='" + full_text + "'}";
  }
}
