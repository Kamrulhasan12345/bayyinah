package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.model.Chapter_i18n;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.infra.local.database.DBExecutor;
import com.ks.bayyinah.controller.cell.VerseCell;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;

import java.util.List;

public class ChaptersController {

  @FXML
  private HBox header;

  @FXML
  private Label nameArabic;

  @FXML
  private Label nameSimple;

  @FXML
  private Label translatedName;

  @FXML
  private ListView<VerseView> verseListView;

  private BrowsingController browsingController;

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  public void showVerses(int chapterId) {
    LocalQuranQueryService quranQueryService = new LocalQuranQueryService();

    DBExecutor.run(() -> {
      // Ensure DB is initialized before querying
      List<VerseView> versesData = quranQueryService.getChapterVerses(chapterId, 20);

      System.out.println(
          "Fetched " +
              versesData.size() +
              " verses for chapter " +
              chapterId);

      ObservableList<VerseView> verses = FXCollections.observableArrayList(versesData);
      Platform.runLater(() -> verseListView.setItems(verses));
    });

    verseListView.setCellFactory(listView -> new VerseCell());
  }

  public void setChapter(ChapterView chapter) {
    Chapter chaptersData = chapter.getChapter();
    Chapter_i18n chapterI18n = chapter.getChapterI18N();
    nameArabic.setText(chaptersData.getNameArabic());
    nameSimple.setText(chaptersData.getNameSimple());
    translatedName.setText(chapterI18n.getTranslatedName());

    showVerses(chaptersData.getId());
  }

}
