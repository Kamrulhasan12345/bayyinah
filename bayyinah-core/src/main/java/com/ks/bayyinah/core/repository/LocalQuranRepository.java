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
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<Chapter_i18n> getChapterInfo(int chapterId, String langCode) {
    return Optional.empty(); // Placeholder return
  }

  @Override
  public Optional<Verse> getVerseByKey(String verseKey) {
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
