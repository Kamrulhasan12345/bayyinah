package com.ks.bayyinah.core.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseSearchResult {
  private VerseView verseView;

  private String highlightedText;
  private double relevanceScore;
  private MatchLocation matchedIn;

  public enum MatchLocation {
    ARABIC_TEXT,
    TRANSLATION,
    VERSE_KEY
  }

  public String getDisplayText() {
    if (matchedIn == MatchLocation.TRANSLATION && verseView.getTranslatedText() != null) {
      return highlightedText != null ? highlightedText : verseView.getTranslatedText();
    }
    return highlightedText != null ? highlightedText : verseView.getArabicText();
  }
}
