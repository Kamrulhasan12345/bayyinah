package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.model.Chapter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ChapterSidebarController {

  @FXML
  private Label chapterId;

  @FXML
  private Label nameArabic;

  @FXML
  private Label nameSimple;

  private ChapterView chapter;

  public void bind(ChapterView chapter) {
    this.chapter = chapter;

    Chapter chapterData = chapter.getChapter();

    chapterId.setText(String.valueOf(chapterData.getId()));
    nameArabic.setText(chapterData.getNameArabic());
    nameSimple.setText(chapterData.getNameSimple());

    // TODO: maybe some other buttons .setOnAction(e -> { ... }) for bookmarking,
    // etc.
  }
}
