package com.ks.bayyinah.infra.local.repository.quran;

import com.ks.bayyinah.core.exception.RepositoryException;
import com.ks.bayyinah.core.model.AudioRecitation;
import com.ks.bayyinah.infra.local.database.DatabaseManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalAudioRecitationRepository {

  public List<AudioRecitation> findAllRecitations() {
    try {
      try (var connection = DatabaseManager.getQuranConnection();
          var statement = connection.prepareStatement(
              "SELECT id, reciter_name, style, translated_name FROM audio_recitations ORDER BY id");
          var resultSet = statement.executeQuery()) {
        List<AudioRecitation> recitations = new ArrayList<>();
        while (resultSet.next()) {
          AudioRecitation recitation = new AudioRecitation();
          recitation.setId(resultSet.getInt("id"));
          recitation.setReciterName(resultSet.getString("reciter_name"));
          recitation.setStyle(resultSet.getString("style"));
          recitation.setTranslatedName(resultSet.getString("translated_name"));
          recitations.add(recitation);
        }
        return recitations;
      }
    } catch (Exception e) {
      throw new RepositoryException("Failed to fetch audio recitations", e);
    }
  }

  public Map<Integer, Integer> countAvailableAudioByRecitation(int chapterId, Integer startVerse, Integer endVerse) {
    boolean hasRange = startVerse != null && endVerse != null;
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT va.recitation_id, COUNT(*) AS available_count ")
        .append("FROM verse_audio va ")
        .append("JOIN verses v ON va.verse_id = v.id ")
        .append("WHERE v.surah_id = ? ");

    if (hasRange) {
      sql.append("AND v.verse_number BETWEEN ? AND ? ");
    }

    sql.append("GROUP BY va.recitation_id");

    try {
      try (var connection = DatabaseManager.getQuranConnection();
          var statement = connection.prepareStatement(sql.toString())) {
        statement.setInt(1, chapterId);
        if (hasRange) {
          statement.setInt(2, startVerse);
          statement.setInt(3, endVerse);
        }

        try (var resultSet = statement.executeQuery()) {
          Map<Integer, Integer> counts = new HashMap<>();
          while (resultSet.next()) {
            counts.put(resultSet.getInt("recitation_id"), resultSet.getInt("available_count"));
          }
          return counts;
        }
      }
    } catch (Exception e) {
      throw new RepositoryException(
          "Failed to count available verse audio by recitation for chapter ID: " + chapterId,
          e);
    }
  }
}
