package com.ks.bayyinah.infra.local.repository;

import com.ks.bayyinah.core.query.QuranReadRepository;
import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.core.model.*;
import com.ks.bayyinah.core.exception.RepositoryException;

import java.util.List;
import java.util.Optional;

public class LocalQuranReadRepository extends LocalRespository implements QuranReadRepository {
  @Override
  public Optional<VerseView> findVerseView(String verseKey, int translationId) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement(
              "SELECT v.id as verse_id, v.surah_id, v.verse_number, v.verse_key, v.text_uthmani, v.text_indopak, t.id as t_id, t.translation_id, t.text "
                  +
                  "FROM verses v " +
                  "LEFT JOIN translation_text t ON v.id = t.verse_id AND t.translation_id = ? " +
                  "WHERE v.verse_key = ?")) {
        statement.setInt(1, translationId);
        statement.setString(2, verseKey);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            VerseView verseView = new VerseView();
            Verse verse = new Verse();
            TranslationText translationText = new TranslationText();
            verse.setId(resultSet.getInt("verse_id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            translationText.setId(resultSet.getInt("t_id"));
            translationText.setTranslationId(resultSet.getInt("translation_id"));
            translationText.setText(resultSet.getString("text"));
            verseView.setVerse(verse);
            verseView.setTranslationText(translationText);
            return Optional.of(verseView);
          }
        }
      }
    } catch (Exception e) {
      // Handle exceptions and return Optional.empty() if verse not found or on error
      throw new RepositoryException(
          "Failed to fetch verse view for verse key: " + verseKey + " and translation ID: " + translationId, e);
    }
    return Optional.empty();
  }

  @Override
  public List<VerseView> findChapterView(int chapterId, int translationId) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement(
              "SELECT v.id as verse_id, v.surah_id, v.verse_number, v.verse_key, v.text_uthmani, v.text_indopak, t.id as t_id, t.translation_id, t.text "
                  +
                  "FROM verses v " +
                  "LEFT JOIN translation_text t ON v.id = t.verse_id AND t.translation_id = ? " +
                  "WHERE v.surah_id = ? ORDER BY v.verse_number")) {
        statement.setInt(1, translationId);
        statement.setInt(2, chapterId);
        try (var resultSet = statement.executeQuery()) {
          List<VerseView> verseViews = new java.util.ArrayList<>();
          while (resultSet.next()) {
            VerseView verseView = new VerseView();
            Verse verse = new Verse();
            TranslationText translationText = new TranslationText();
            verse.setId(resultSet.getInt("verse_id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            translationText.setId(resultSet.getInt("t_id"));
            translationText.setTranslationId(resultSet.getInt("translation_id"));
            translationText.setText(resultSet.getString("text"));
            verseView.setVerse(verse);
            verseView.setTranslationText(translationText);
            verseViews.add(verseView);
          }
          return verseViews;
        }
      }
    } catch (Exception e) {
      // Handle exceptions and return empty list on error
      throw new RepositoryException(
          "Failed to fetch chapter view for chapter ID: " + chapterId + " and translation ID: " + translationId, e);
    }
  }

  @Override
  public Page<VerseView> findChapterView(int chapterId, int translationId, PageRequest pageRequest) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement(
              "SELECT v.id as verse_id, v.surah_id, v.verse_number, v.verse_key, v.text_uthmani, v.text_indopak, t.id as t_id, t.translation_id, t.text "
                  +
                  "FROM verses v " +
                  "LEFT JOIN translation_text t ON v.id = t.verse_id AND t.translation_id = ? " +
                  "WHERE v.surah_id = ? ORDER BY v.verse_number LIMIT ? OFFSET ?")) {
        statement.setInt(1, translationId);
        statement.setInt(2, chapterId);
        statement.setInt(3, pageRequest.getPageSize());
        statement.setInt(4, (pageRequest.getPage() - 1) * pageRequest.getPageSize());
        int totalElements = countVersesByChapter(chapterId);
        try (var resultSet = statement.executeQuery()) {
          List<VerseView> verseViews = new java.util.ArrayList<>();
          while (resultSet.next()) {
            VerseView verseView = new VerseView();
            Verse verse = new Verse();
            TranslationText translationText = new TranslationText();
            verse.setId(resultSet.getInt("verse_id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            translationText.setId(resultSet.getInt("t_id"));
            translationText.setTranslationId(resultSet.getInt("translation_id"));
            translationText.setText(resultSet.getString("text"));
            verseView.setVerse(verse);
            verseView.setTranslationText(translationText);
            verseViews.add(verseView);
          }
          return new Page<>(verseViews, pageRequest.getPage(), pageRequest.getPageSize(), totalElements);
        }
      }
    } catch (Exception e) {
      // Handle exceptions and return empty page on error
      throw new RepositoryException(
          "Failed to fetch paginated chapter view for chapter ID: " + chapterId + " and translation ID: "
              + translationId,
          e);
    }
  }

  @Override
  public List<ChapterView> findAllChapters(String langCode) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement(
              "SELECT c.id as chapter_id, c.name_simple, c.name_arabic, c.verse_count, c.revelation_place, t.translated_name, t.full_text, t.short_text, t.id as i18n_id"
                  +
                  " FROM chapters c " +
                  "LEFT JOIN chapters_i18n t ON c.id = t.chapter_id AND t.lang_code = ? " +
                  "ORDER BY c.id")) {
        statement.setString(1, langCode);
        try (var resultSet = statement.executeQuery()) {
          List<ChapterView> chapterViews = new java.util.ArrayList<>();
          while (resultSet.next()) {
            ChapterView chapterView = new ChapterView();
            Chapter chapter = new Chapter();
            Chapter_i18n chapterI18N = new Chapter_i18n();
            chapter.setId(resultSet.getInt("chapter_id"));
            chapter.setNameSimple(resultSet.getString("name_simple"));
            chapter.setNameArabic(resultSet.getString("name_arabic"));
            chapter.setVerseCount(resultSet.getInt("verse_count"));
            chapter.setRevelationPlace(resultSet.getString("revelation_place"));
            chapterI18N.setId(resultSet.getInt("i18n_id"));
            chapterI18N.setChapterId(resultSet.getInt("chapter_id"));
            chapterI18N.setLangCode(langCode);
            chapterI18N.setTranslatedName(resultSet.getString("translated_name"));
            chapterI18N.setShortText(resultSet.getString("short_text"));
            chapterI18N.setFullText(resultSet.getString("full_text"));
            chapterView.setChapter(chapter);
            chapterView.setChapterI18N(chapterI18N);
            chapterViews.add(chapterView);
          }
          return chapterViews;
        }
      }
    } catch (Exception e) {
      // Handle exceptions and return empty list on error
      throw new RepositoryException("Failed to fetch all chapters with translations for language code: " + langCode,
          e);
    }
  }

  @Override
  public Optional<ChapterView> findChapterById(int chapterId, String langCode) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement(
              "SELECT c.id as chapter_id, c.name_simple, c.name_arabic, c.verse_count, c.revelation_place, t.translated_name, t.full_text, t.short_text, t.id as i18n_id"
                  +
                  " FROM chapters c " +
                  "LEFT JOIN chapters_i18n t ON c.id = t.chapter_id AND t.lang_code = ? " +
                  "WHERE c.id = ?")) {
        statement.setString(1, langCode);
        statement.setInt(2, chapterId);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            ChapterView chapterView = new ChapterView();
            Chapter chapter = new Chapter();
            Chapter_i18n chapterI18N = new Chapter_i18n();
            chapter.setId(resultSet.getInt("chapter_id"));
            chapter.setNameSimple(resultSet.getString("name_simple"));
            chapter.setNameArabic(resultSet.getString("name_arabic"));
            chapter.setVerseCount(resultSet.getInt("verse_count"));
            chapter.setRevelationPlace(resultSet.getString("revelation_place"));
            chapterI18N.setId(resultSet.getInt("i18n_id"));
            chapterI18N.setChapterId(resultSet.getInt("chapter_id"));
            chapterI18N.setLangCode(langCode);
            chapterI18N.setTranslatedName(resultSet.getString("translated_name"));
            chapterI18N.setShortText(resultSet.getString("short_text"));
            chapterI18N.setFullText(resultSet.getString("full_text"));
            chapterView.setChapter(chapter);
            chapterView.setChapterI18N(chapterI18N);
            return Optional.of(chapterView);
          }
        }
      }
    } catch (Exception e) {
      // Handle exceptions and return Optional.empty() on error
      throw new RepositoryException(
          "Failed to fetch chapter view for chapter ID: " + chapterId + " and language code: " + langCode, e);
    }
    return Optional.empty();
  }

  private int countVersesByChapter(int chapterId) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement("SELECT COUNT(*) FROM verses WHERE surah_id = ?")) {
        statement.setInt(1, chapterId);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getInt(1);
          }
        }
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RepositoryException("Failed to count verses for chapter ID: " + chapterId, e);
    }
    return 0;
  }
}
