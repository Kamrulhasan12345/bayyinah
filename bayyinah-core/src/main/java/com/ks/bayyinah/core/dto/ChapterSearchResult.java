package com.ks.bayyinah.core.dto;

import lombok.*;

/**
 * Search result wrapping ChapterView with search metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterSearchResult {
  private ChapterView chapterView;

  private String highlightedName;
  private double relevanceScore;

  public String getDisplayName() {
    if (highlightedName != null) {
      return highlightedName;
    }

    if (chapterView.getChapterI18N() != null) {
      return chapterView.getChapterI18N().getTranslatedName();
    }

    return chapterView.getChapter().getNameSimple();
  }
}
