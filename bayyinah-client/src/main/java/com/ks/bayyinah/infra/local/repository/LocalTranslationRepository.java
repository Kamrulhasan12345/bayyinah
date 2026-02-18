package com.ks.bayyinah.infra.local.repository;

import com.ks.bayyinah.core.repository.TranslationRepository;
import com.ks.bayyinah.core.model.Translation;
import com.ks.bayyinah.core.exception.RepositoryException;

import java.util.Optional;
import java.util.List;

public class LocalTranslationRepository extends LocalRespository implements TranslationRepository {
  // Fetches all translations available in the app
  @Override
  public List<Translation> findAllTranslations() {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement("SELECT * FROM translations");
          var resultSet = statement.executeQuery()) {
        List<Translation> translations = new java.util.ArrayList<>();
        while (resultSet.next()) {
          Translation translation = new Translation();
          translation.setId(resultSet.getInt("id"));
          translation.setAuthorName(resultSet.getString("author_name"));
          translation.setLanguage(resultSet.getString("language"));
          translations.add(translation);
        }
        return translations;
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RepositoryException("Failed to fetch translations", e);
    }
  }

  // Fetches a translation by its unique ID
  @Override
  public Optional<Translation> findTranslationById(int translationId) {
    try {
      try (var connection = getConnection();
          var statement = connection.prepareStatement("SELECT * FROM translations WHERE id = ?")) {
        statement.setInt(1, translationId);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            Translation translation = new Translation();
            translation.setId(resultSet.getInt("id"));
            translation.setAuthorName(resultSet.getString("author_name"));
            translation.setLanguage(resultSet.getString("language"));
            return Optional.of(translation);
          }
        }
      }
    } catch (Exception e) {
      // Handle exceptions related to database access
      throw new RepositoryException("Failed to fetch translation by ID: " + translationId, e);
    }
    return Optional.empty();
  }

}
