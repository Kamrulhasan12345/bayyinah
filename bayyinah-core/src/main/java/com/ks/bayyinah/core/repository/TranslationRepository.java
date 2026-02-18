package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.model.Translation;

import java.util.List;
import java.util.Optional;

public interface TranslationRepository {

  // Fetches all available translations (e.g., "Sahih International", "Yusuf Ali")
  List<Translation> findAllTranslations();

  // Fetches a specific translation by its ID
  Optional<Translation> findTranslationById(int translationId);
}
