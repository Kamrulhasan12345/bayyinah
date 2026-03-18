package com.ks.bayyinah.infra.local.repository.quran;

import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.core.model.*;
import com.ks.bayyinah.core.repository.SearchRepository;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalSearchRepository implements SearchRepository {

  // Verse key patterns: "2:255", "18:10-15", "1:1"
  private static final Pattern VERSE_KEY_PATTERN = Pattern.compile(
      "^(\\d{1,3}):(\\d{1,3})(?:-(\\d{1,3}))?$");

  @Override
  public Optional<VerseView> searchByVerseKey(String verseKey, Integer translationId) {
    // Parse verse key
    Matcher matcher = VERSE_KEY_PATTERN.matcher(verseKey.trim());
    if (!matcher.matches()) {
      return Optional.empty();
    }

    int surahId = Integer.parseInt(matcher.group(1));
    int verseNumber = Integer.parseInt(matcher.group(2));

    System.out
        .println("Searching for verse key: " + verseKey + " (Surah: " + surahId + ", Verse: " + verseNumber + ")");

    String sql;
    if (translationId != null) {
      sql = """
              SELECT
                  v.id, v.surah_id, v.verse_number, v.verse_key,
                  v.text_uthmani, v.text_indopak,
                  tt.id as tt_id, tt.text as translation_text
              FROM verses v
              LEFT JOIN translation_text tt ON v.id = tt.verse_id AND tt.translation_id = ?
              WHERE v.surah_id = ? AND v.verse_number = ?
          """;
    } else {
      sql = """
              SELECT
                  v.id, v.surah_id, v.verse_number, v.verse_key,
                  v.text_uthmani, v.text_indopak
              FROM verses v
              WHERE v.surah_id = ? AND v.verse_number = ?
          """;
    }

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      if (translationId != null) {
        stmt.setInt(1, translationId);
        stmt.setInt(2, surahId);
        stmt.setInt(3, verseNumber);
      } else {
        stmt.setInt(1, surahId);
        stmt.setInt(2, verseNumber);
      }

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          System.out.println("Verse found for key: " + verseKey);
          return Optional.of(mapToVerseView(rs, translationId));
        }
      }

    } catch (SQLException e) {
      throw new RepositoryException("Failed to search by verse key: " + verseKey, e);
    }

    return Optional.empty();
  }

  @Override
  public Page<VerseSearchResult> searchInArabicText(String query, PageRequest pageRequest) {
    // Clean query (remove diacritics for better matching)
    String cleanQuery = removeDiacritics(query);

    // Count total results
    long totalCount = countArabicSearchResults(cleanQuery);

    if (totalCount == 0) {
      return Page.empty();
    }

    String sql = """
            SELECT
                v.id, v.surah_id, v.verse_number, v.verse_key,
                v.text_uthmani, v.text_indopak,
                bm25(verses_fts) as rank
            FROM verses_fts
            JOIN verses v ON verses_fts.verse_id = v.id
            WHERE verses_fts MATCH ?
            ORDER BY rank
            LIMIT ? OFFSET ?
        """;

    List<VerseSearchResult> results = new ArrayList<>();

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, cleanQuery);
      stmt.setInt(2, pageRequest.getPageSize());
      stmt.setInt(3, pageRequest.getOffset());

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          VerseView verseView = mapToVerseView(rs, null);

          VerseSearchResult searchResult = VerseSearchResult.builder()
              .verseView(verseView)
              .matchedIn(VerseSearchResult.MatchLocation.ARABIC_TEXT)
              .relevanceScore(rs.getDouble("rank"))
              .highlightedText(highlightArabic(verseView.getArabicText(), query))
              .build();

          results.add(searchResult);
        }
      }

    } catch (SQLException e) {
      throw new RepositoryException("Failed to search in Arabic text: " + query, e);
    }

    return new Page<>(results, pageRequest.getPage(), pageRequest.getPageSize(), (int) totalCount);
  }

  @Override
  public Page<VerseSearchResult> searchInTranslation(
      String query,
      int translationId,
      PageRequest pageRequest) {
    // Count total results
    long totalCount = countTranslationSearchResults(query, translationId);

    if (totalCount == 0) {
      return Page.empty();
    }

    String sql = """
            SELECT
                v.id, v.surah_id, v.verse_number, v.verse_key,
                v.text_uthmani, v.text_indopak,
                tt.id as tt_id, tt.text as translation_text,
                bm25(translation_text_fts) as rank
            FROM translation_text_fts
            JOIN translation_text tt ON translation_text_fts.id = tt.id
            JOIN verses v ON tt.verse_id = v.id
            WHERE translation_text_fts MATCH ? AND translation_text_fts.translation_id = ?
            ORDER BY rank
            LIMIT ? OFFSET ?
        """;

    List<VerseSearchResult> results = new ArrayList<>();

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, query);
      stmt.setInt(2, translationId);
      stmt.setInt(3, pageRequest.getPageSize());
      stmt.setInt(4, pageRequest.getOffset());

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          VerseView verseView = mapToVerseView(rs, translationId);

          VerseSearchResult searchResult = VerseSearchResult.builder()
              .verseView(verseView)
              .matchedIn(VerseSearchResult.MatchLocation.TRANSLATION)
              .relevanceScore(rs.getDouble("rank"))
              .highlightedText(highlightText(verseView.getTranslatedText(), query))
              .build();

          results.add(searchResult);
        }
      }

    } catch (SQLException e) {
      throw new RepositoryException("Failed to search in translation: " + query, e);
    }

    return new Page<>(results, pageRequest.getPage(), pageRequest.getPageSize(), (int) totalCount);
  }

  @Override
  public Page<VerseSearchResult> searchEverywhere(
      String query,
      Integer translationId,
      PageRequest pageRequest) {
    List<VerseSearchResult> allResults = new ArrayList<>();

    // Search in Arabic (get all matching, not paginated yet)
    PageRequest largeRequest = PageRequest.of(1, 1000);
    Page<VerseSearchResult> arabicResults = searchInArabicText(query, largeRequest);
    allResults.addAll(arabicResults.getContent());

    // Search in translation if provided
    if (translationId != null) {
      Page<VerseSearchResult> translationResults = searchInTranslation(query, translationId, largeRequest);
      allResults.addAll(translationResults.getContent());
    }

    // Sort by relevance (lower bm25/relevanceScore means more relevant)
    allResults.sort((a, b) -> Double.compare(a.getRelevanceScore(), b.getRelevanceScore()));

    // Manual pagination
    int start = pageRequest.getOffset();
    int end = Math.min(start + pageRequest.getPageSize(), allResults.size());

    if (start >= allResults.size()) {
      return new Page<>(new ArrayList<>(), pageRequest.getPage(), pageRequest.getPageSize(), allResults.size());
    }

    List<VerseSearchResult> pageResults = allResults.subList(start, end);

    return new Page<>(pageResults, pageRequest.getPage(), pageRequest.getPageSize(), allResults.size());
  }

  @Override
  public Page<ChapterSearchResult> searchChapters(
      String query,
      String langCode,
      PageRequest pageRequest) {
    // Count total
    long totalCount = countChapterSearchResults(query, langCode);

    if (totalCount == 0) {
      return Page.empty();
    }

    String sql;

    if (langCode != null) {
      sql = """
              SELECT DISTINCT
                  c.id, c.name_simple, c.name_arabic, c.verse_count, c.revelation_place,
                  ci.id as ci_id, ci.chapter_id, ci.lang_code, ci.translated_name,
                  ci.short_text, ci.full_text
              FROM chapters c
              LEFT JOIN chapters_i18n ci ON c.id = ci.chapter_id AND ci.lang_code = ?
              WHERE c.name_simple LIKE ? OR c.name_arabic LIKE ?
                 OR ci.translated_name LIKE ? OR ci.short_text LIKE ?
              ORDER BY c.id
              LIMIT ? OFFSET ?
          """;
    } else {
      sql = """
              SELECT
                  c.id, c.name_simple, c.name_arabic, c.verse_count, c.revelation_place
              FROM chapters c
              WHERE c.name_simple LIKE ? OR c.name_arabic LIKE ?
              ORDER BY c.id
              LIMIT ? OFFSET ?
          """;
    }

    List<ChapterSearchResult> results = new ArrayList<>();
    String searchPattern = "%" + query + "%";

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      int paramIndex = 1;

      if (langCode != null) {
        stmt.setString(paramIndex++, langCode);
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex++, searchPattern);
      } else {
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex++, searchPattern);
      }

      stmt.setInt(paramIndex++, pageRequest.getPageSize());
      stmt.setInt(paramIndex, pageRequest.getOffset());

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          ChapterView chapterView = mapToChapterView(rs, langCode != null);

          ChapterSearchResult searchResult = ChapterSearchResult.builder()
              .chapterView(chapterView)
              .highlightedName(highlightText(chapterView.getChapter().getNameSimple(), query))
              .relevanceScore(1.0) // Simple matching, no BM25
              .build();

          results.add(searchResult);
        }
      }

    } catch (SQLException e) {
      throw new RepositoryException("Failed to search chapters: " + query, e);
    }

    return new Page<>(results, pageRequest.getPage(), pageRequest.getPageSize(), (int) totalCount);
  }

  @Override
  public Page<VerseView> getVersesByChapter(
      int chapterId,
      Integer translationId,
      PageRequest pageRequest) {
    // Count total verses in chapter
    long totalCount = countVersesByChapter(chapterId);

    if (totalCount == 0) {
      return Page.empty();
    }

    String sql;

    if (translationId != null) {
      sql = """
              SELECT
                  v.id, v.surah_id, v.verse_number, v.verse_key,
                  v.text_uthmani, v.text_indopak,
                  tt.id as tt_id, tt.text as translation_text
              FROM verses v
              LEFT JOIN translation_text tt ON v.id = tt.verse_id AND tt.translation_id = ?
              WHERE v.surah_id = ?
              ORDER BY v.verse_number
              LIMIT ? OFFSET ?
          """;
    } else {
      sql = """
              SELECT
                  v.id, v.surah_id, v.verse_number, v.verse_key,
                  v.text_uthmani, v.text_indopak
              FROM verses v
              WHERE v.surah_id = ?
              ORDER BY v.verse_number
              LIMIT ? OFFSET ?
          """;
    }

    List<VerseView> results = new ArrayList<>();

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      int paramIndex = 1;

      if (translationId != null) {
        stmt.setInt(paramIndex++, translationId);
      }

      stmt.setInt(paramIndex++, chapterId);
      stmt.setInt(paramIndex++, pageRequest.getPageSize());
      stmt.setInt(paramIndex, pageRequest.getOffset());

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(mapToVerseView(rs, translationId));
        }
      }

    } catch (SQLException e) {
      throw new RepositoryException("Failed to get verses by chapter: " + chapterId, e);
    }

    return new Page<>(results, pageRequest.getPage(), pageRequest.getPageSize(), (int) totalCount);
  }

  @Override
  public SearchResponse smartSearch(
      String query,
      int translationId,
      PageRequest pageRequest) {
    query = query.trim();

    // 1. Check if it's a verse key (e.g., "2:255")
    if (VERSE_KEY_PATTERN.matcher(query).matches()) {
      Optional<VerseView> verse = searchByVerseKey(query, translationId);

      if (verse.isPresent()) {
        VerseSearchResult result = VerseSearchResult.builder()
            .verseView(verse.get())
            .matchedIn(VerseSearchResult.MatchLocation.VERSE_KEY)
            .relevanceScore(1.0)
            .build();

        Page<VerseSearchResult> page = new Page<>(
            List.of(result),
            1,
            1,
            1);

        return SearchResponse.builder()
            .query(query)
            .searchType(SearchResponse.SearchType.VERSE_KEY)
            .verseResults(page)
            .build();
      }
      return SearchResponse.builder()
          .query(query)
          .searchType(SearchResponse.SearchType.VERSE_KEY)
          .verseResults(Page.empty())
          .build();
    }

    // 2. Check if it's a chapter name search (if short query)
    // if (query.length() <= 20) { // Chapter names are usually short
    // Page<ChapterSearchResult> chapterResults = searchChapters(query, "en",
    // PageRequest.first(5));
    //
    // if (!chapterResults.isEmpty()) {
    // return SearchResponse.builder()
    // .query(query)
    // .searchType(SearchResponse.SearchType.CHAPTERS)
    // .chapterResults(chapterResults)
    // .build();
    // }
    // }

    // 3. Full-text search in verses
    Page<VerseSearchResult> verseResults = searchInTranslation(query, translationId, pageRequest);

    return SearchResponse.builder()
        .query(query)
        .searchType(SearchResponse.SearchType.VERSES)
        .verseResults(verseResults)
        .build();
  }

  // ═══════════════════════════════════════════════════════════
  // HELPER METHODS - Mapping
  // ═══════════════════════════════════════════════════════════

  private VerseView mapToVerseView(ResultSet rs, Integer translationId) throws SQLException {
    Verse verse = new Verse();
    verse.setId(rs.getInt("id"));
    verse.setSurahId(rs.getInt("surah_id"));
    verse.setVerseNumber(rs.getInt("verse_number"));
    verse.setVerseKey(rs.getString("verse_key"));
    verse.setTextUthmani(rs.getString("text_uthmani"));
    verse.setTextIndopak(rs.getString("text_indopak"));

    TranslationText translationText = null;
    if (translationId != null) {
      int ttId = rs.getInt("tt_id");
      if (!rs.wasNull()) {
        translationText = new TranslationText();
        translationText.setId(ttId);
        translationText.setText(rs.getString("translation_text"));
        translationText.setVerseId(verse.getId());
        translationText.setTranslationId(translationId);

      }
    }

    return new VerseView(verse, translationText);
  }

  private ChapterView mapToChapterView(ResultSet rs, boolean hasI18n) throws SQLException {
    Chapter chapter = new Chapter();
    chapter.setId(rs.getInt("id"));
    chapter.setNameSimple(rs.getString("name_simple"));
    chapter.setNameArabic(rs.getString("name_arabic"));
    chapter.setVerseCount(rs.getInt("verse_count"));
    chapter.setRevelationPlace(rs.getString("revelation_place"));

    Chapter_i18n i18n = null;
    if (hasI18n) {
      int ciId = rs.getInt("ci_id");
      if (!rs.wasNull()) {
        i18n = new Chapter_i18n();
        i18n.setId(ciId);
        i18n.setChapterId(rs.getInt("chapter_id"));
        i18n.setLangCode(rs.getString("lang_code"));
        i18n.setTranslatedName(rs.getString("translated_name"));
        i18n.setShortText(rs.getString("short_text"));
        i18n.setFullText(rs.getString("full_text"));
      }
    }

    return new ChapterView(chapter.getId(), chapter, i18n);
  }

  // ═══════════════════════════════════════════════════════════
  // HELPER METHODS - Counting
  // ═══════════════════════════════════════════════════════════

  private long countArabicSearchResults(String query) {
    String sql = "SELECT COUNT(*) FROM verses_fts WHERE verses_fts MATCH ?";

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, query);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("Failed to count Arabic search results", e);
    }

    return 0;
  }

  private long countTranslationSearchResults(String query, int translationId) {
    String sql = """
            SELECT COUNT(*)
            FROM translation_text_fts
            JOIN translation_text tt ON translation_text_fts.id = tt.id
            WHERE translation_text_fts MATCH ? AND translation_text_fts.translation_id = ?
        """;

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, query);
      stmt.setInt(2, translationId);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("Failed to count translation search results", e);
    }

    return 0;
  }

  private long countChapterSearchResults(String query, String langCode) {
    String sql;
    String searchPattern = "%" + query + "%";

    if (langCode != null) {
      sql = """
              SELECT COUNT(DISTINCT c.id)
              FROM chapters c
              LEFT JOIN chapters_i18n ci ON c.id = ci.chapter_id AND ci.lang_code = ?
              WHERE c.name_simple LIKE ? OR c.name_arabic LIKE ?
                 OR ci.translated_name LIKE ? OR ci.short_text LIKE ?
          """;
    } else {
      sql = """
              SELECT COUNT(*)
              FROM chapters c
              WHERE c.name_simple LIKE ? OR c.name_arabic LIKE ?
          """;
    }

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      int paramIndex = 1;

      if (langCode != null) {
        stmt.setString(paramIndex++, langCode);
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex, searchPattern);
      } else {
        stmt.setString(paramIndex++, searchPattern);
        stmt.setString(paramIndex, searchPattern);
      }

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("Failed to count chapter search results", e);
    }

    return 0;
  }

  private long countVersesByChapter(int chapterId) {
    String sql = "SELECT COUNT(*) FROM verses WHERE surah_id = ?";

    try (Connection conn = DatabaseManager.getQuranConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, chapterId);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
      }
    } catch (SQLException e) {
      throw new RepositoryException("Failed to count verses by chapter", e);
    }

    return 0;
  }

  // ═══════════════════════════════════════════════════════════
  // HELPER METHODS - Text Processing
  // ═══════════════════════════════════════════════════════════

  private String removeDiacritics(String text) {
    return text.replaceAll("[\\u064B-\\u0652\\u0670]", "");
  }

  private String highlightArabic(String text, String searchTerm) {
    if (text == null || searchTerm == null) {
      return text;
    }

    String cleanSearch = removeDiacritics(searchTerm);
    String cleanText = removeDiacritics(text);

    int index = cleanText.indexOf(cleanSearch);
    if (index >= 0) {
      int start = index;
      int end = index + cleanSearch.length();
      return text.substring(0, start) + "**" + text.substring(start, end) + "**" + text.substring(end);
    }

    return text;
  }

  private String highlightText(String text, String searchTerm) {
    if (text == null || searchTerm == null) {
      return text;
    }

    Pattern pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(text);

    if (matcher.find()) {
      return matcher.replaceAll("**$0**");
    }

    return text;
  }
}
