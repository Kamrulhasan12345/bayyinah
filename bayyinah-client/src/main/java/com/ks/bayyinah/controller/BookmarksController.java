package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.infra.hybrid.model.Bookmark;
import com.ks.bayyinah.infra.hybrid.service.BookmarkService;
import com.ks.bayyinah.infra.hybrid.service.UserPreferenceService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.ui.ToastManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BookmarksController {

  private static final String CHAPTER_LANGUAGE = "en";
  private static final int FALLBACK_TRANSLATION_ID = 20;

  @FXML
  private Label summaryLabel;

  @FXML
  private ListView<BookmarkRow> bookmarksListView;

  private AppContext appContext;
  private BrowsingController browsingController;

  void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  void initializeBookmarks() {
    configureListView();
    loadBookmarks();
  }

  private void configureListView() {
    Label placeholder = new Label("No bookmarks yet.");
    placeholder.getStyleClass().add("bookmarks-empty");
    bookmarksListView.setPlaceholder(placeholder);

    bookmarksListView.setCellFactory(listView -> new ListCell<>() {
      @Override
      protected void updateItem(BookmarkRow item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          setOnMouseClicked(null);
          return;
        }

        VBox root = new VBox(6);
        root.getStyleClass().add("bookmark-row");

        HBox topRow = new HBox(10);
        topRow.getStyleClass().add("bookmark-row-top");

        Label surahLabel = new Label(item.surahLabel());
        surahLabel.getStyleClass().add("bookmark-surah");
        HBox.setHgrow(surahLabel, Priority.ALWAYS);

        Label keyLabel = new Label(item.verseKeyLabel());
        keyLabel.getStyleClass().add("bookmark-key");

        topRow.getChildren().addAll(surahLabel, keyLabel);

        Label arabicLabel = new Label(item.arabicText());
        arabicLabel.getStyleClass().add("bookmark-arabic");
        arabicLabel.setWrapText(true);

        root.getChildren().addAll(topRow, arabicLabel);

        setText(null);
        setGraphic(root);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        setOnMouseClicked(event -> {
          BookmarkRow row = getItem();
          if (row == null) {
            return;
          }
          openBookmark(row);
        });
      }
    });
  }

  private void loadBookmarks() {
    summaryLabel.setText("Loading bookmarks...");

    DbAsync.runWithUi(
        this::buildPayload,
        payload -> {
          bookmarksListView.getItems().setAll(payload.rows());
          int count = payload.rows().size();
          if (count == 0) {
            summaryLabel.setText("No bookmarked verses yet.");
          } else {
            summaryLabel.setText(count + " bookmarked verses");
          }
        },
        err -> {
          err.printStackTrace();
          summaryLabel.setText("Unable to load bookmarks right now.");
        });
  }

  private BookmarkPayload buildPayload() {
    BookmarkService bookmarkService = appContext != null ? appContext.getBookmarkService() : null;
    List<Bookmark> bookmarks = bookmarkService != null ? bookmarkService.getAll() : List.of();

    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();
    List<ChapterView> chapters = quranQueryService.getAllChapters(CHAPTER_LANGUAGE);
    Map<Integer, ChapterView> chaptersById = new HashMap<>();
    for (ChapterView chapterView : chapters) {
      if (chapterView == null || chapterView.getChapter() == null) {
        continue;
      }
      chaptersById.put(chapterView.getChapter().getId(), chapterView);
    }

    int translationId = resolveTranslationId();
    List<BookmarkRow> rows = new ArrayList<>();

    for (Bookmark bookmark : bookmarks) {
      if (bookmark == null || bookmark.getSurahNumber() <= 0 || bookmark.getAyahNumber() <= 0) {
        continue;
      }

      int surahNumber = bookmark.getSurahNumber();
      int ayahNumber = bookmark.getAyahNumber();
      ChapterView chapterView = chaptersById.get(surahNumber);
      String surahLabel = buildSurahLabel(chapterView, surahNumber);

      String verseKey = surahNumber + ":" + ayahNumber;
      String arabicText = quranQueryService.getVerse(verseKey, translationId)
          .map(VerseView::getArabicText)
          .filter(text -> text != null && !text.isBlank())
          .orElse("Arabic text unavailable.");

      rows.add(new BookmarkRow(chapterView, surahNumber, ayahNumber, surahLabel, arabicText));
    }

    return new BookmarkPayload(rows);
  }

  private int resolveTranslationId() {
    if (appContext == null) {
      return FALLBACK_TRANSLATION_ID;
    }

    UserPreferenceService userPreferenceService = appContext.getUserPreferenceService();
    if (userPreferenceService == null) {
      return FALLBACK_TRANSLATION_ID;
    }

    try {
      return userPreferenceService.getDefaultTranslation();
    } catch (Exception ex) {
      return FALLBACK_TRANSLATION_ID;
    }
  }

  private String buildSurahLabel(ChapterView chapterView, int surahNumber) {
    if (chapterView == null || chapterView.getChapter() == null) {
      return "Surah " + surahNumber;
    }

    Chapter chapter = chapterView.getChapter();
    String simpleName = chapter.getNameSimple();
    if (simpleName == null || simpleName.isBlank()) {
      return "Surah " + surahNumber;
    }

    return surahNumber + ". " + simpleName;
  }

  private void openBookmark(BookmarkRow row) {
    if (browsingController == null) {
      return;
    }

    ChapterView chapterView = row.chapterView();
    if (chapterView == null) {
      chapterView = LocalQuranQueryService.getInstance().getChapter(row.surahNumber(), CHAPTER_LANGUAGE).orElse(null);
    }

    if (chapterView == null) {
      ToastManager.getInstance().showWarning(
          "Bookmark unavailable",
          "Unable to open Surah " + row.surahNumber() + ", ayah " + row.ayahNumber() + ".");
      return;
    }

    browsingController.showChapterAndFocusAyah(chapterView, row.ayahNumber());
  }

  private record BookmarkPayload(List<BookmarkRow> rows) {
  }

  private record BookmarkRow(
      ChapterView chapterView,
      int surahNumber,
      int ayahNumber,
      String surahLabel,
      String arabicText) {

    String verseKeyLabel() {
      return "Ayah " + ayahNumber;
    }
  }
}
