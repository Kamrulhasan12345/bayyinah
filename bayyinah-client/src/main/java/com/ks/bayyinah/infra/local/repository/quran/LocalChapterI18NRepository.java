package com.ks.bayyinah.infra.local.repository.quran;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.core.model.Chapter_i18n;
import com.ks.bayyinah.core.repository.ChapterI18NRepository;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

import java.util.Optional;

public class LocalChapterI18NRepository implements ChapterI18NRepository {
  @Override
  public Optional<Chapter_i18n> getChapterInfo(int chapterId, String langCode) {
    try {
      try (var connection = DatabaseManager.getQuranConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM chapters_i18n WHERE chapter_id = ? AND lang_code = ?")) {
        statement.setInt(1, chapterId);
        statement.setString(2, langCode);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            Chapter_i18n chapterI18N = new Chapter_i18n();
            chapterI18N.setId(resultSet.getInt("id"));
            chapterI18N.setChapterId(resultSet.getInt("chapter_id"));
            chapterI18N.setLangCode(resultSet.getString("lang_code"));
            chapterI18N.setTranslatedName(resultSet.getString("translated_name"));
            chapterI18N.setShortText(resultSet.getString("short_text"));
            chapterI18N.setFullText(resultSet.getString("full_text"));
            return Optional.of(chapterI18N);
          }
        }
      }
    } catch (Exception e) {
      throw new RepositoryException(
          "Failed to fetch chapter i18n info for chapter ID: " + chapterId + " and language code: " + langCode, e);
    }
    return Optional.empty();
  }
}
