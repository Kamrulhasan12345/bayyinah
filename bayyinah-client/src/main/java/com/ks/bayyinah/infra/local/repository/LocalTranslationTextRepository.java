package com.ks.bayyinah.infra.local.repository;

import com.ks.bayyinah.core.repository.TranslationTextRepository;
import com.ks.bayyinah.core.model.TranslationText;
import com.ks.bayyinah.core.dto.Page;
import com.ks.bayyinah.core.dto.PageRequest;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class LocalTranslationTextRepository extends LocalRespository implements TranslationTextRepository {
  // Fetches specific translation text for a verse
  @Override
  public Optional<TranslationText> findTranslation(int verseId, int translationId) {
    try {
      try (var connection = getConnection();
          var statement = connection
              .prepareStatement("SELECT * FROM translation_text WHERE verse_id = ? AND translation_id = ?")) {
        statement.setInt(1, verseId);
        statement.setInt(2, translationId);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            TranslationText translationText = new TranslationText();
            translationText.setId(resultSet.getInt("id"));
            translationText.setVerseId(resultSet.getInt("verse_id"));
            translationText.setTranslationId(resultSet.getInt("translation_id"));
            translationText.setText(resultSet.getString("text"));
            return Optional.of(translationText);
          }
        }
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RuntimeException(
          "Failed to fetch translation for verse ID: " + verseId + " and translation ID: " + translationId, e);
    }
    return Optional.empty();
  }

  @Override
  public List<TranslationText> findTranslationsByChapterId(int chapterId) {
    try {
      try (var connection = getConnection();
          var pstmt = connection.prepareStatement("SELECT tt.* from translation_text tt " +
              "JOIN verses v ON tt.verse_id = v.id " + "WHERE surah_id = ?");) {
        pstmt.setInt(1, chapterId);
        try (var resultSet = pstmt.executeQuery()) {
          List<TranslationText> translations = new ArrayList<>();
          while (resultSet.next()) {
            TranslationText translationText = new TranslationText();
            translationText.setId(resultSet.getInt("id"));
            translationText.setVerseId(resultSet.getInt("verse_id"));
            translationText.setTranslationId(resultSet.getInt("translation_id"));
            translationText.setText(resultSet.getString("text"));
            translations.add(translationText);
          }
          return translations;
        }
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RuntimeException("Failed to fetch translations for chapter ID: " + chapterId, e);
    }
  }

  @Override
  public Page<TranslationText> findTranslationsByChapterId(int chapterId, PageRequest pageRequest) {
    try {
      try (var connection = getConnection();
          var pstmt = connection.prepareStatement("SELECT tt.* from translation_text tt" +
              "JOIN verses v ON tt.verse_id = v.id" + "WHERE surah_id = ? LIMIT ? OFFSET ?");) {
        pstmt.setInt(1, chapterId);
        pstmt.setInt(2, pageRequest.getPageSize());
        pstmt.setInt(3, pageRequest.getPage() * pageRequest.getPageSize());
        int totalElements = countTranslationsByChapterId(chapterId);
        try (var resultSet = pstmt.executeQuery()) {
          List<TranslationText> translations = new ArrayList<>();
          while (resultSet.next()) {
            TranslationText translationText = new TranslationText();
            translationText.setId(resultSet.getInt("id"));
            translationText.setVerseId(resultSet.getInt("verse_id"));
            translationText.setTranslationId(resultSet.getInt("translation_id"));
            translationText.setText(resultSet.getString("text"));
            translations.add(translationText);
          }
          return totalElements > 0 ? new Page<TranslationText>(translations, pageRequest.getPage(), pageRequest.getPageSize(), totalElements) : Page.empty();
        }
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RuntimeException("Failed to fetch translations for chapter ID: " + chapterId, e);
    }
  }

  private int countTranslationsByChapterId(int chapterId) {
    try {
      try (var connection = getConnection();
          var pstmt = connection.prepareStatement("SELECT COUNT(*) as total FROM translation_text tt" +
              "JOIN verses v ON tt.verse_id = v.id" + "WHERE surah_id = ?");) {
        pstmt.setInt(1, chapterId);
        try (var resultSet = pstmt.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getInt("total");
          }
        }
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RuntimeException("Failed to count translations for chapter ID: " + chapterId, e);
    }
    return 0;
  }
}
