package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.model.Chapter_i18n;

import java.util.Optional;

public interface ChapterI18NRepository {

  // Fetches localized name/info (e.g., Bengali name for Chapter 1)
  Optional<Chapter_i18n> getChapterInfo(int chapterId, String langCode);
}
