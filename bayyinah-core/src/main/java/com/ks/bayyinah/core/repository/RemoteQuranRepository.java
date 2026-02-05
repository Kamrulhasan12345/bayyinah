package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.models.*;
import com.ks.bayyinah.core.dto.VerseView;

import java.util.List;
import java.util.Optional;

public class RemoteQuranRepository implements QuranRepository {
  private String apiBaseUrl;

  public RemoteQuranRepository(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  @Override
  public List<Chapter> getAllChapters() {
    // Implementation to fetch chapters from remote API
    return List.of(); // Placeholder return
  }

  @Override
  public Optional<Chapter> getChapterById(int chapterId) {
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<Chapter_i18n> getChapterInfo(int chapterId, String langCode) {
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<Verse> getVerseByKey(String verseKey) {
    return Optional.empty(); // Placeholder return
  }

  @Override
  public List<Verse> getVersesByChapter(int chapterId) {
    return List.of(); // Placeholder return
  }

  @Override
  public List<Translation> getAvailableTranslations() {
    return List.of(); // Placeholder return
  }

  @Override
  public Optional<TranslationText> getTranslation(int verseId, int translationId) {
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<VerseView> getVerseWithTranslation(String verseKey, int translationId) {
    return Optional.empty(); // Placeholder return
  }

  @Override
  public List<VerseView> getChapterWithTranslation(int chapterId, int translationId) {
    return List.of(); // Placeholder return
  }
}
