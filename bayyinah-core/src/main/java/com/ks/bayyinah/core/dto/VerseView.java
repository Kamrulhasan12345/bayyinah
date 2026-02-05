package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.models.Verse;
import com.ks.bayyinah.core.models.Translation;

public class VerseView {
  private Verse arabic;
  private Translation translation;

  public VerseView(Verse arabic, Translation translation) {
    this.arabic = arabic;
    this.translation = translation;
  }
};
