package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.controller.cell.VerseCell;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.model.Chapter_i18n;
import com.ks.bayyinah.infra.hybrid.model.UserPreference;
import com.ks.bayyinah.infra.hybrid.service.UserPreferenceService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import lombok.Setter;

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

  @Setter
  private AppContext appContext;

  private Runnable onLoadComplete;

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  public void setOnLoadComplete(Runnable callback) {
    this.onLoadComplete = callback;
  }

  public void showVerses(int chapterId) {
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();
    UserPreferenceService userPreferencesService = appContext.getUserPreferenceService();

    DbAsync.runWithUi(
        () -> quranQueryService.getChapterVerses(chapterId, userPreferencesService.getDefaultTranslation()), verses -> {
          System.out.println("Fetched " + verses.size() + " verses for chapter " + chapterId);
          verseListView.setItems(FXCollections.observableArrayList(verses));
          System.out.println("Loaded " + verses.size() + " verses");
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        }, e -> {
          e.printStackTrace();
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        });

    verseListView.setCellFactory(listView -> new VerseCell());
  }

  public void showVerses(int chapterId, int startVerse, int endVerse) {
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();
    UserPreferenceService userPreferencesService = appContext.getUserPreferenceService();

    DbAsync.runWithUi(
        () -> quranQueryService.getVersesByRange(chapterId, startVerse, endVerse,
            userPreferencesService.getDefaultTranslation()),
        verses -> {
          System.out.println("Fetched " + verses.size() + " verses for chapter " + chapterId);
          verseListView.setItems(FXCollections.observableArrayList(verses));
          System.out.println("Loaded " + verses.size() + " verses");
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        }, e -> {
          e.printStackTrace();
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        });

    verseListView.setCellFactory(listView -> new VerseCell());
  }

  public void setChapter(ChapterView chapter, Integer startVerse, Integer endVerse, Integer translationId) {
    Chapter chaptersData = chapter.getChapter();
    Chapter_i18n chapterI18n = chapter.getChapterI18N();
    nameArabic.setText(chaptersData.getNameArabic());
    nameSimple.setText(chaptersData.getNameSimple());
    translatedName.setText(chapterI18n.getTranslatedName());

    if (startVerse == null || endVerse == null)
      showVerses(chaptersData.getId());
    else
      showVerses(chaptersData.getId(), startVerse, endVerse);
  }
}
