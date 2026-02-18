package com.ks.bayyinah.infra.local.repository;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.repository.ChapterRepository;

import java.util.Optional;
import java.util.List;

public class LocalChapterRepository extends LocalRespository implements ChapterRepository {
  // Fetches a chapter by its unique ID
  @Override
  public Optional<Chapter> findChapterById(int chapterId) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement("SELECT * FROM chapters WHERE id = ?")) {
        statement.setInt(1, chapterId);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            Chapter chapter = new Chapter();
            chapter.setId(resultSet.getInt("id"));
            chapter.setNameSimple(resultSet.getString("name_simple"));
            chapter.setNameArabic(resultSet.getString("name_arabic"));
            chapter.setVerseCount(resultSet.getInt("verse_count"));
            chapter.setRevelationPlace(resultSet.getString("revelation_place"));
            return Optional.of(chapter);
          }
        }
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RepositoryException("Failed to fetch chapter by ID: " + chapterId, e);
    }
    return Optional.empty();
  }

  // Fetches all chapters in the Quran
  @Override
  public List<Chapter> findAllChapters() {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement("SELECT * FROM chapters");
          var resultSet = statement.executeQuery()) {
        List<Chapter> chapters = new java.util.ArrayList<>();
        while (resultSet.next()) {
          Chapter chapter = new Chapter();
          chapter.setId(resultSet.getInt("id"));
          chapter.setNameSimple(resultSet.getString("name_simple"));
          chapter.setNameArabic(resultSet.getString("name_arabic"));
          chapter.setVerseCount(resultSet.getInt("verse_count"));
          chapter.setRevelationPlace(resultSet.getString("revelation_place"));
          chapters.add(chapter);
        }
        return chapters;
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RepositoryException("Failed to fetch all chapters", e);
    }
  }
}
