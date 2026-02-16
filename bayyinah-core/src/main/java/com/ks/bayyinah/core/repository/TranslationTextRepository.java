package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.model.TranslationText;
import com.ks.bayyinah.core.dto.Page;
import com.ks.bayyinah.core.dto.PageRequest;

import java.util.List;
import java.util.Optional;

public interface TranslationTextRepository {

  // Fetches specific translation text for a verse
  Optional<TranslationText> findTranslation(int verseId, int translationId);

  List<TranslationText> findTranslationsByChapterId(int chapterId);

  Page<TranslationText> findTranslationsByChapterId(int chapterId, PageRequest pageRequest);
}
