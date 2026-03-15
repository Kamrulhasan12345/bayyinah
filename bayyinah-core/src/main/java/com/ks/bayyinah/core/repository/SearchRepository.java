package com.ks.bayyinah.core.repository;

import java.util.Optional;

import com.ks.bayyinah.core.dto.*;

public interface SearchRepository {

  /**
   * Search by verse key (e.g., "2:255", "18:10", "1:1-7")
   * 
   * @param verseKey      The verse reference
   * @param translationId Translation to include (null = Arabic only)
   * @return Optional containing verse if found
   */
  Optional<VerseView> searchByVerseKey(String verseKey, Integer translationId);

  /**
   * Search in Arabic text (Uthmani or Indopak) with pagination
   * 
   * @param query       Search term in Arabic
   * @param pageRequest Pagination parameters
   * @return Page of matching verses ordered by relevance
   */
  Page<VerseSearchResult> searchInArabicText(String query, PageRequest pageRequest);

  /**
   * Search in translation text with pagination
   * 
   * @param query         Search term
   * @param translationId Translation to search in (required)
   * @param pageRequest   Pagination parameters
   * @return Page of matching verses ordered by relevance
   */
  Page<VerseSearchResult> searchInTranslation(
      String query,
      int translationId,
      PageRequest pageRequest);

  /**
   * Search in both Arabic and translation with pagination
   * 
   * @param query         Search term
   * @param translationId Translation to include in results (null = Arabic only)
   * @param pageRequest   Pagination parameters
   * @return Page of matching verses ordered by relevance
   */
  Page<VerseSearchResult> searchEverywhere(
      String query,
      Integer translationId,
      PageRequest pageRequest);

  /**
   * Search for chapters/surahs by name with pagination
   * 
   * @param query       Chapter name (Arabic, English, or translated)
   * @param langCode    Language code for i18n search (null = all)
   * @param pageRequest Pagination parameters
   * @return Page of matching chapters
   */
  Page<ChapterSearchResult> searchChapters(
      String query,
      String langCode,
      PageRequest pageRequest);

  /**
   * Get all verses in a specific chapter with pagination
   * 
   * @param chapterId     Chapter ID
   * @param translationId Translation ID (null = Arabic only)
   * @param pageRequest   Pagination parameters
   * @return Page of verses in chapter
   */
  Page<VerseView> getVersesByChapter(
      int chapterId,
      Integer translationId,
      PageRequest pageRequest);

  /**
   * Smart search - automatically detects search type
   * Examples:
   * - "2:255" → verse key search
   * - "Al-Fatiha" → chapter search
   * - "Allah" → full-text search
   * 
   * @param query         User's search query
   * @param translationId Translation to include (null = Arabic only)
   * @param pageRequest   Pagination parameters
   * @return Unified search response
   */
  SearchResponse smartSearch(
      String query,
      Integer translationId,
      PageRequest pageRequest);
}
