package com.ks.bayyinah.infra.local.query;

import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.core.model.*;
import com.ks.bayyinah.core.query.*;
import com.ks.bayyinah.core.repository.*;
import com.ks.bayyinah.infra.local.repository.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LocalQuranQueryService implements QuranQueryService {

  private static LocalQuranQueryService instance;

  private VerseRepository verseRepository; // Local repository for verses
  private ChapterRepository chapterRepository; // Local repository for chapters
  private TranslationTextRepository translationTextRepository; // Local repository for translations
  private TranslationRepository translationRepository; // Local repository for translation metadata
  private ChapterI18NRepository chapterI18NRepository; // Local repository for chapter internationalization
  private QuranReadRepository quranReadRepository; // Local repository for Quran read operations

  public LocalQuranQueryService() {
    // Initialize local data sources (e.g., database connections, DAOs, etc.)
    this.verseRepository = new LocalVerseRepository();
    this.chapterRepository = new LocalChapterRepository();
    this.translationTextRepository = new LocalTranslationTextRepository();
    this.translationRepository = new LocalTranslationRepository();
    this.chapterI18NRepository = new LocalChapterI18NRepository();
    this.quranReadRepository = new LocalQuranReadRepository();
  }

  public static synchronized LocalQuranQueryService getInstance() {
    if (instance == null) {
      instance = new LocalQuranQueryService();
    }
    return instance;
  }

  @Override
  public List<ChapterView> getAllChapters(String langCode) {
    // Implement logic to fetch all chapters with localized names/info from local
    return quranReadRepository.findAllChapters(langCode); // Placeholder
  }

  public List<ChapterView> searchChapter(String keyword, String langCode) {
    List<ChapterView> chapterViews = quranReadRepository.findAllChapters(
        langCode);
    if (keyword == null || keyword.isBlank()) {
      return chapterViews;
    }

    String loweredKeyword = keyword.toLowerCase();
    return chapterViews
        .stream()
        .filter(chapterView -> {
          Chapter chapter = chapterView.getChapter();
          String nameSimple = chapter != null ? chapter.getNameSimple() : null;
          return (nameSimple != null &&
              nameSimple.toLowerCase().contains(loweredKeyword));
        })
        .collect(Collectors.toList());
  }

  @Override
  public Optional<ChapterView> getChapter(int chapterId, String langCode) {
    // Implement logic to fetch specific chapter details with localization from
    // local data source
    return quranReadRepository.findChapterById(chapterId, langCode);
  }

  @Override
  public Optional<VerseView> getVerse(String verseKey, int translationId) {
    // Implement logic to fetch specific verse details with translation and
    // localization from local data source
    return quranReadRepository.findVerseView(verseKey, translationId); // Placeholder
  }

  @Override
  public List<VerseView> getChapterVerses(int chapterId, int translationId) {
    // Implement logic to fetch all verses of a chapter with translation and
    // localization from local data source
    return quranReadRepository.findChapterView(chapterId, translationId); // Placeholder
  }

  @Override
  public Page<VerseView> getChapterVerses(
      int chapterId,
      int translationId,
      PageRequest pageRequest) {
    // Implement logic to fetch verses of a chapter with pagination, translation,
    // and localization from local data source
    return quranReadRepository.findChapterView(
        chapterId,
        translationId,
        pageRequest); // Placeholder
  }

  @Override
  public List<TranslationView> getAvailableTranslations() {
    // Implement logic to fetch all available translations from local data source
    List<Translation> translations = translationRepository.findAllTranslations();
    List<TranslationView> translationViews = new ArrayList<>();
    translations.forEach(translation -> {
      TranslationView view = new TranslationView();
      view.setId(translation.getId());
      view.setTranslation(translation);
      translationViews.add(view);
    });
    return translationViews; // Placeholder
  }

  public void saveTranslation(int translationId) {
    // Implement logic to save a translation to the local data source
  }
}
