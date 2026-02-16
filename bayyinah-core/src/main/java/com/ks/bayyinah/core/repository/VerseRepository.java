package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.model.Verse;
import com.ks.bayyinah.core.dto.Page;
import com.ks.bayyinah.core.dto.PageRequest;

import java.util.List;
import java.util.Optional;

public interface VerseRepository {
  // Fetches a verse by its unique key (e.g., "2:255")
  Optional<Verse> findVerseByKey(String verseKey);

  // Fetches a verse by its unique ID
  Optional<Verse> findVerseById(int verseId);

  // Fetches a verse by its chapter ID and verse number
  Optional<Verse> findVerseByChapterAndVerseNumber(int chapterId, int verseNumber);

  // Fetches all verses for a given chapter
  List<Verse> findVersesByChapter(int chapterId);

  // Fetches verses for a chapter with pagination support
  Page<Verse> findVersesByChapter(int chapterId, PageRequest pageRequest);
}
