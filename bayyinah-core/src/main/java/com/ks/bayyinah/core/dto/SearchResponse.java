package com.ks.bayyinah.core.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
  private String query;
  private SearchType searchType;

  private Page<VerseSearchResult> verseResults;
  private Page<ChapterSearchResult> chapterResults;

  public enum SearchType {
    VERSE_KEY, // Direct verse reference (e.g., "2:255")
    VERSES, // Full-text search in verses
    CHAPTERS, // Search in chapter names
    MIXED // Both verses and chapters (future)
  }

  public long getTotalResults() {
    if (verseResults != null) return verseResults.getTotalElements();
    if (chapterResults != null) return chapterResults.getTotalElements();
    return 0;
  }

  public static SearchResponse empty(String query, SearchType type) {
    return SearchResponse.builder()
        .query(query)
        .searchType(type)
        .verseResults(type == SearchType.VERSES ? Page.empty() : null)
        .chapterResults(type == SearchType.CHAPTERS ? Page.empty() : null)
        .build();
  }
}
