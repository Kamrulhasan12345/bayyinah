package com.ks.bayyinah.core.query;

import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.dto.TranslationView;
import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.dto.Page;
import com.ks.bayyinah.core.dto.PageRequest;

import java.util.List;
import java.util.Optional;

public interface QuranQueryService {

  /* -------- Chapter-level -------- */

  List<ChapterView> getAllChapters(String langCode);

  Optional<ChapterView> getChapter(int chapterId, String langCode);

  /* -------- Verse-level -------- */

  Optional<VerseView> getVerse(
      String verseKey,
      int translationId,
      String langCode);

  List<VerseView> getChapterVerses(
      int chapterId,
      int translationId,
      String langCode);

  Page<VerseView> getChapterVerses(
      int chapterId,
      int translationId,
      String langCode,
      PageRequest pageRequest);

  /* -------- Translation -------- */

  List<TranslationView> getAvailableTranslations();
}
