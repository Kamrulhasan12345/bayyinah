package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.model.Verse;
import com.ks.bayyinah.core.model.TranslationText;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class VerseView {
  private Verse verse;
  private TranslationText translationText;

  public String getArabicText() {
    return verse != null ? verse.getText() : null;
  }

  public String getTranslatedText() {
    return translationText != null ? translationText.getText() : null;
  }

  public String getVerseKey() {
    return verse != null ? verse.getVerseKey() : null;
  }
};
