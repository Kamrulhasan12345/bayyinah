package com.ks.bayyinah.infra.local.repository.quran;

import com.ks.bayyinah.core.repository.VerseRepository;
import com.ks.bayyinah.core.model.Verse;
import com.ks.bayyinah.core.dto.Page;
import com.ks.bayyinah.core.dto.PageRequest;
import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.infra.local.repository.LocalRepository;

import java.util.List;
import java.util.Optional;

public class LocalVerseRepository extends LocalRepository implements VerseRepository {
  // Fetches a verse by its unique key (e.g., "2:255")
  @Override
  public Optional<Verse> findVerseByKey(String verseKey) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement("SELECT * FROM verses WHERE verse_key = ?")) {
        statement.setString(1, verseKey);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            Verse verse = new Verse();
            verse.setId(resultSet.getInt("id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            return Optional.of(verse);
          }
        }
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to fetch verse by key: " + verseKey, e);
    }
    return Optional.empty(); // Placeholder implementation
  }

  // Fetches a verse by its unique ID
  @Override
  public Optional<Verse> findVerseById(int verseId) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement("SELECT * FROM verses WHERE id = ?")) {
        statement.setInt(1, verseId);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            Verse verse = new Verse();
            verse.setId(resultSet.getInt("id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            return Optional.of(verse);
          }
        }
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to fetch verse by ID: " + verseId, e);
    }
    return Optional.empty(); // Placeholder implementation
  }

  // Fetches a verse by its chapter ID and verse number
  @Override
  public Optional<Verse> findVerseByChapterAndVerseNumber(int chapterId, int verseNumber) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM verses WHERE surah_id = ? AND verse_number = ?")) {
        statement.setInt(1, chapterId);
        statement.setInt(2, verseNumber);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            Verse verse = new Verse();
            verse.setId(resultSet.getInt("id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            return Optional.of(verse);
          }
        }
      }
    } catch (Exception e) {
      throw new RepositoryException(
          "Failed to fetch verse by chapter ID: " + chapterId + " and verse number: " + verseNumber, e);
    }
    return Optional.empty(); // Placeholder implementation
  }

  // Fetches all verses for a given chapter
  @Override
  public List<Verse> findVersesByChapter(int chapterId) {
    try {
      try (var connection = getConnection();
          var statement = connection
              .prepareStatement("SELECT * FROM verses WHERE surah_id = ? ORDER BY verse_number")) {
        statement.setInt(1, chapterId);
        try (var resultSet = statement.executeQuery()) {
          List<Verse> verses = new java.util.ArrayList<>();
          while (resultSet.next()) {
            Verse verse = new Verse();
            verse.setId(resultSet.getInt("id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            verses.add(verse);
          }
          return verses;
        }
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to fetch verses for chapter ID: " + chapterId, e);
    }
  }

  // Fetches verses for a chapter with pagination support
  @Override
  public Page<Verse> findVersesByChapter(int chapterId, PageRequest pageRequest) {
    try {
      int offset = (pageRequest.getPage() - 1) * pageRequest.getPageSize();
      try (var connection = getConnection();
          var statement = connection.prepareStatement(
              "SELECT * FROM verses WHERE surah_id = ? ORDER BY verse_number LIMIT ? OFFSET ?")) {
        statement.setInt(1, chapterId);
        statement.setInt(2, pageRequest.getPageSize());
        statement.setInt(3, offset);
        int totalElements = countVersesByChapter(chapterId);
        try (var resultSet = statement.executeQuery()) {
          List<Verse> verses = new java.util.ArrayList<>();
          while (resultSet.next()) {
            Verse verse = new Verse();
            verse.setId(resultSet.getInt("id"));
            verse.setSurahId(resultSet.getInt("surah_id"));
            verse.setVerseNumber(resultSet.getInt("verse_number"));
            verse.setVerseKey(resultSet.getString("verse_key"));
            verse.setTextUthmani(resultSet.getString("text_uthmani"));
            verse.setTextIndopak(resultSet.getString("text_indopak"));
            verses.add(verse);
          }
          return totalElements > 0 ? new Page<>(verses, pageRequest.getPage(), pageRequest.getPageSize(), totalElements)
              : Page.empty();
        }
      }
    } catch (Exception e) {
      throw new RepositoryException(
          "Failed to fetch verses for chapter ID: " + chapterId + " with pagination", e);
    }
  }

  private int countVersesByChapter(int chapterId) {
    try (var connection = getConnection();
        var statement = connection.prepareStatement("SELECT COUNT(*) FROM verses WHERE surah_id = ?")) {
      statement.setInt(1, chapterId);
      try (var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1);
        }
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to count verses for chapter ID: " + chapterId, e);
    }
    return 0;
  }
}
