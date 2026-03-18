package com.ks.bayyinah.core.query;

import com.ks.bayyinah.core.dto.*;

import java.util.List;
import java.util.Optional;

public interface QuranReadRepository {
  Optional<VerseView> findVerseView(String verseKey, int translationId);

  List<VerseView> findChapterView(int chapterId, int translationId);

  Page<VerseView> findChapterView(int chapterId, int translationId, PageRequest pageRequest);

  List<ChapterView> findAllChapters(String langCode);

  Optional<ChapterView> findChapterById(int chapterId, String langCode);

  List<VerseView> findVersesByRange(int chapterId, int startVerse, int endVerse, int translationId);

  Page<VerseView> findVersesByRange(int chapterId, int startVerse, int endVerse, int translationId, PageRequest pageRequest);
}
