package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.model.Chapter_i18n;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ChapterSidebarController {

  @FXML
  private Label chapterId;

  @FXML
  private Label nameArabic;

  @FXML
  private Label nameSimple;

  @FXML
  private Label translatedName;

  @FXML
  private Label chapterMeta;

  private ChapterView chapter;

  public void bind(ChapterView chapter) {
    this.chapter = chapter;

    Chapter chapterData = chapter.getChapter();
    Chapter_i18n chapterI18n = chapter.getChapterI18N();

    chapterId.setText(String.valueOf(chapterData.getId()));
    nameArabic.setText(chapterData.getNameArabic() != null ? chapterData.getNameArabic() : "-");
    nameSimple.setText(chapterData.getNameSimple() != null ? chapterData.getNameSimple() : "Unknown");
    translatedName.setText(resolveTranslatedName(chapterData, chapterI18n));
    chapterMeta.setText(formatChapterMeta(chapterData));

    // TODO: maybe some other buttons .setOnAction(e -> { ... }) for bookmarking,
    // etc.
  }

  private String resolveTranslatedName(Chapter chapterData, Chapter_i18n chapterI18n) {
    if (chapterI18n != null && chapterI18n.getTranslatedName() != null && !chapterI18n.getTranslatedName().isBlank()) {
      return chapterI18n.getTranslatedName();
    }
    if (chapterData.getNameSimple() != null && !chapterData.getNameSimple().isBlank()) {
      return chapterData.getNameSimple();
    }
    return "Untitled Chapter";
  }

  private String formatChapterMeta(Chapter chapterData) {
    int verseCount = chapterData.getVerseCount();
    String revelationPlace = chapterData.getRevelationPlace() != null && !chapterData.getRevelationPlace().isBlank()
        ? chapterData.getRevelationPlace()
        : "Unknown";
    return verseCount + " ayahs · " + revelationPlace;
  }

}
