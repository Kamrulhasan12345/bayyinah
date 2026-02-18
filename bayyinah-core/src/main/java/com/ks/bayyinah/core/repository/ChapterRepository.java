package com.ks.bayyinah.core.repository;

import com.ks.bayyinah.core.model.Chapter;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository {

  // Fetches a chapter by its unique ID
  Optional<Chapter> findChapterById(int chapterId);

  // Fetches all chapters in the Quran
  List<Chapter> findAllChapters();
}
