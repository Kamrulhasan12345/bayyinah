package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.model.Chapter_i18n;

public class ChapterView {
  private int id;
  private Chapter chapter;
  private Chapter_i18n chapter_i18n;

  public ChapterView(int id, Chapter chapter, Chapter_i18n chapter_i18n) {
    this.id = id;
    this.chapter = chapter;
    this.chapter_i18n = chapter_i18n;
  }

  public int getId() {
    return id;
  }

  public Chapter getChapter() {
    return chapter;
  }

  public Chapter_i18n getChapter_i18n() {
    return chapter_i18n;
  }
}
