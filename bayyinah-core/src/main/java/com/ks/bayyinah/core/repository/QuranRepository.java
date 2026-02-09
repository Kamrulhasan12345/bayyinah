package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.models.*;
import com.ks.bayyinah.core.dto.VerseView;

import java.util.List;
import java.util.Optional;

public interface QuranRepository {

  // --- Chapter Operations ---
  List<Chapter> getAllChapters();

  Optional<Chapter> getChapterById(int chapterId);

  // Fetches localized name/info (e.g., Bengali name for Chapter 1)
  Optional<Chapter_i18n> getChapterInfo(int chapterId, String langCode);

  // --- Verse Operations ---
  Optional<Verse> getVerseByKey(String verseKey);

  List<Verse> getVersesByChapter(int chapterId);

  List<Verse> getPaginatedVersesByChapter(int chapterId, int pageNumber, int pageSize);

  // --- Translation Operations ---
  List<Translation> getAvailableTranslations();

  // Fetches specific translation text for a verse
  Optional<TranslationText> getTranslation(int verseId, int translationId);

  // --- The "UI Hero" Method ---
  // This combines Verse and TranslationText into the VerseView you built
  Optional<VerseView> getVerseWithTranslation(String verseKey, int translationId);

  // Returns a full chapter with a specific translation (Great for the reader
  // view)
  List<VerseView> getChapterWithTranslation(int chapterId, int translationId);

  List<VerseView> getPaginatedChapterWithTranslation(int chapterId, int translationId, int pageNumber, int pageSize);
}
