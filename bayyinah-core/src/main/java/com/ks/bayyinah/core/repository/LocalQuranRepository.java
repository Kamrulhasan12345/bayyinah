package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.models.*;
import com.ks.bayyinah.core.dto.VerseView;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.sql.*;

public class LocalQuranRepository implements QuranRepository {
  private String databasePath;

  public LocalQuranRepository(String databasePath) {
    this.databasePath = databasePath;
  }

  @Override
  public List<Chapter> getAllChapters() {
    // Implementation to fetch chapters from local database
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        PreparedStatement pstmt = connection
            .prepareStatement("SELECT * FROM chapters")) {
      // Execute SQL query to fetch chapters and map results to Chapter objects

      try (ResultSet rs = pstmt.executeQuery()) {
        List<Chapter> chapters = new ArrayList<>();
        while (rs.next()) {
          int id = rs.getInt("id");
          String name = rs.getString("name_simple");
          String name_arabic = rs.getString("name_arabic");
          int verse_count = rs.getInt("verse_count");
          String revelation_place = rs.getString("revelation_place");
          chapters.add(new Chapter(id, name, name_arabic, verse_count, revelation_place));
        }
        return chapters;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return List.of(); // Placeholder return
  }

  @Override
  public Optional<Chapter> getChapterById(int chapterId) {
    try {
      // Implementation to fetch a chapter by ID from local database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection
              .prepareStatement("SELECT * FROM chapters WHERE id = ?")) {
        pstmt.setInt(1, chapterId);
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name_simple");
            String name_arabic = rs.getString("name_arabic");
            int verse_count = rs.getInt("verse_count");
            String revelation_place = rs.getString("revelation_place");
            return Optional.of(new Chapter(id, name, name_arabic, verse_count, revelation_place));
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<Chapter_i18n> getChapterInfo(int chapterId, String langCode) {
    try {
      // Implementation to fetch localized chapter info from local database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection
              .prepareStatement("SELECT * FROM chapter_i18n WHERE chapter_id = ? AND lang_code = ?")) {
        pstmt.setInt(1, chapterId);
        pstmt.setString(2, langCode);
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            int id = rs.getInt("id");
            int chapter_id = rs.getInt("chapter_id");
            String lang_code = rs.getString("lang_code");
            String translated_name = rs.getString("translated_name");
            String short_text = rs.getString("short_text");
            String full_text = rs.getString("full_text");
            return Optional.of(new Chapter_i18n(id, chapter_id, lang_code, translated_name, short_text, full_text));
          }
        }
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<Verse> getVerseByKey(String verseKey) {
    // Implementation to fetch a verse by its key from local database
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        PreparedStatement pstmt = connection
            .prepareStatement("SELECT * FROM verses WHERE verse_key = ?")) {
      pstmt.setString(1, verseKey);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          int id = rs.getInt("id");
          int surah_id = rs.getInt("surah_id");
          int verse_number = rs.getInt("verse_number");
          String verse_key = rs.getString("verse_key");
          String text_uthmani = rs.getString("text_uthmani");
          String text_indopak = rs.getString("text_indopak");
          return Optional.of(new Verse(id, surah_id, verse_number, verse_key, text_uthmani, text_indopak));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty(); // Placeholder return
  }

  @Override
  public List<Verse> getVersesByChapter(int chapterId) {
    try {
      // Implementation to fetch verses by chapter ID from local database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection
              .prepareStatement("SELECT * FROM verses WHERE surah_id = ? ORDER BY verse_number")) {
        pstmt.setInt(1, chapterId);
        try (ResultSet rs = pstmt.executeQuery()) {
          List<Verse> verses = new ArrayList<>();
          while (rs.next()) {
            int id = rs.getInt("id");
            int surah_id = rs.getInt("surah_id");
            int verse_number = rs.getInt("verse_number");
            String verse_key = rs.getString("verse_key");
            String text_uthmani = rs.getString("text_uthmani");
            String text_indopak = rs.getString("text_indopak");
            verses.add(new Verse(id, surah_id, verse_number, verse_key, text_uthmani, text_indopak));
          }
          return verses;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return List.of(); // Placeholder return
  }

  @Override
  public List<Verse> getPaginatedVersesByChapter(int chapterId, int pageNumber, int pageSize) {
    try {
      // Implementation to fetch paginated verses by chapter ID from local database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection.prepareStatement(
              "SELECT * FROM verses WHERE surah_id = ? ORDER BY verse_number LIMIT ? OFFSET ?")) {
        pstmt.setInt(1, chapterId);
        pstmt.setInt(2, pageSize);
        pstmt.setInt(3, (pageNumber - 1) * pageSize);
        try (ResultSet rs = pstmt.executeQuery()) {
          List<Verse> verses = new ArrayList<>();
          while (rs.next()) {
            int id = rs.getInt("id");
            int surah_id = rs.getInt("surah_id");
            int verse_number = rs.getInt("verse_number");
            String verse_key = rs.getString("verse_key");
            String text_uthmani = rs.getString("text_uthmani");
            String text_indopak = rs.getString("text_indopak");
            verses.add(new Verse(id, surah_id, verse_number, verse_key, text_uthmani, text_indopak));
          }
          return verses;
        }
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return List.of(); // Placeholder return
  }

  @Override
  public List<Translation> getAvailableTranslations() {
    try {
      // Implementation to fetch available translations from local database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection
              .prepareStatement("SELECT * FROM translations")) {
        try (ResultSet rs = pstmt.executeQuery()) {
          List<Translation> translations = new ArrayList<>();
          while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String language = rs.getString("language");
            translations.add(new Translation(id, name, language));
          }
          return translations;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return List.of(); // Placeholder return
  }

  @Override
  public Optional<TranslationText> getTranslation(int verseId, int translationId) {
    try {
      // Implementation to fetch specific translation text for a verse from local
      // database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection.prepareStatement(
              "SELECT * FROM translation_text WHERE verse_id = ? AND translation_id = ?")) {
        pstmt.setInt(1, verseId);
        pstmt.setInt(2, translationId);
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            int verse_id = rs.getInt("verse_id");
            int translation_id = rs.getInt("translation_id");
            String text = rs.getString("text");
            return Optional.of(new TranslationText(verse_id, translation_id, text));
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<VerseView> getVerseWithTranslation(String verseKey, int translationId) {
    try {
      // Implementation to fetch a verse with its translation from local database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection.prepareStatement(
              "SELECT v.*, t.text AS translation_text FROM verses v LEFT JOIN translation_text t ON v.id = t.verse_id AND t.translation_id = ? WHERE v.verse_key = ?")) {
        pstmt.setInt(1, translationId);
        pstmt.setString(2, verseKey);
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            int id = rs.getInt("id");
            int surah_id = rs.getInt("surah_id");
            int verse_number = rs.getInt("verse_number");
            String verse_key = rs.getString("verse_key");
            String text_uthmani = rs.getString("text_uthmani");
            String text_indopak = rs.getString("text_indopak");
            String translation_text = rs.getString("translation_text");
            Verse verse = new Verse(id, surah_id, verse_number, verse_key, text_uthmani, text_indopak);
            TranslationText translation = new TranslationText(id, translationId, translation_text);
            return Optional.of(new VerseView(verse, translation));
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty(); // Placeholder return
  }

  @Override
  public List<VerseView> getChapterWithTranslation(int chapterId, int translationId) {
    try {
      // Implementation to fetch a full chapter with a specific translation from local
      // database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection.prepareStatement(
              "SELECT v.*, t.text AS translation_text FROM verses v LEFT JOIN translation_text t ON v.id = t.verse_id AND t.translation_id = ? WHERE v.surah_id = ? ORDER BY v.verse_number")) {
        pstmt.setInt(1, translationId);
        pstmt.setInt(2, chapterId);
        try (ResultSet rs = pstmt.executeQuery()) {
          List<VerseView> verseViews = new ArrayList<>();
          while (rs.next()) {
            int id = rs.getInt("id");
            int surah_id = rs.getInt("surah_id");
            int verse_number = rs.getInt("verse_number");
            String verse_key = rs.getString("verse_key");
            String text_uthmani = rs.getString("text_uthmani");
            String text_indopak = rs.getString("text_indopak");
            String translation_text = rs.getString("translation_text");
            Verse verse = new Verse(id, surah_id, verse_number, verse_key, text_uthmani, text_indopak);
            //
            TranslationText translation = new TranslationText(id, translationId, translation_text);
            // // ID is not used here
            verseViews.add(new VerseView(verse, translation));
          }
          return verseViews;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return List.of(); // Placeholder return
  }

  @Override
  public List<VerseView> getPaginatedChapterWithTranslation(int chapterId, int translationId, int pageNumber,
      int pageSize) {
    try {
      // Implementation to fetch paginated chapter with translation from local
      // database
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
          PreparedStatement pstmt = connection.prepareStatement(
              "SELECT v.*, t.text AS translation_text FROM verses v LEFT JOIN translation_text t ON v.id = t.verse_id AND t.translation_id = ? WHERE v.surah_id = ? ORDER BY v.verse_number LIMIT ? OFFSET ?")) {
        pstmt.setInt(1, translationId);
        pstmt.setInt(2, chapterId);
        pstmt.setInt(3, pageSize);
        pstmt.setInt(4, (pageNumber - 1) * pageSize);
        try (ResultSet rs = pstmt.executeQuery()) {
          List<VerseView> verseViews = new ArrayList<>();
          while (rs.next()) {
            int id = rs.getInt("id");
            int surah_id = rs.getInt("surah_id");
            int verse_number = rs.getInt("verse_number");
            String verse_key = rs.getString("verse_key");
            String text_uthmani = rs.getString("text_uthmani");
            String text_indopak = rs.getString("text_indopak");
            String translation_text = rs.getString("translation_text");
            Verse verse = new Verse(id, surah_id, verse_number, verse_key, text_uthmani, text_indopak);
            TranslationText translation = new TranslationText(id, translationId, translation_text);
            verseViews.add(new VerseView(verse, translation));
          }
          return verseViews;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return List.of(); // Placeholder return
  }
}
