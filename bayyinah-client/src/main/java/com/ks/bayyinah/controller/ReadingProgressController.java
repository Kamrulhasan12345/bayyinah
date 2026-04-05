package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.infra.hybrid.model.ReadingProgress;
import com.ks.bayyinah.infra.hybrid.service.ReadingProgressService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ReadingProgressController {

  private static final String CHAPTER_LANGUAGE = "en";
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @FXML
  private Label summaryLabel;

  @FXML
  private ListView<SurahProgressRow> progressListView;

  private AppContext appContext;
  private BrowsingController browsingController;

  void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  void initializeReadingProgress() {
    configureListView();
    loadReadingProgress();
  }

  private void configureListView() {
    Label placeholder = new Label("No chapters found.");
    placeholder.getStyleClass().add("reading-progress-empty");
    progressListView.setPlaceholder(placeholder);

    progressListView.setCellFactory(listView -> new ListCell<>() {
      @Override
      protected void updateItem(SurahProgressRow item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          setOnMouseClicked(null);
          return;
        }

        VBox root = new VBox(8);
        root.getStyleClass().add("progress-row");

        HBox header = new HBox(10);
        header.getStyleClass().add("progress-row-header");

        VBox titleMetaBox = new VBox(3);
        HBox.setHgrow(titleMetaBox, Priority.ALWAYS);

        Label surahLabel = new Label(item.chapterNumber() + ". " + item.simpleName());
        surahLabel.getStyleClass().add("progress-surah");

        if (!item.arabicName().isBlank()) {
          Label arabicLabel = new Label(item.arabicName());
          arabicLabel.getStyleClass().add("progress-arabic");
          titleMetaBox.getChildren().addAll(surahLabel, arabicLabel);
        } else {
          titleMetaBox.getChildren().add(surahLabel);
        }

        Label percentLabel = new Label(item.percentLabel());
        percentLabel.getStyleClass().add("progress-percent");

        header.getChildren().addAll(titleMetaBox, percentLabel);

        ProgressBar progressBar = new ProgressBar(item.completionRatio());
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("progress-row-bar");

        Label metaLabel = new Label(buildMetaLine(item));
        metaLabel.getStyleClass().add("progress-meta");
        metaLabel.setWrapText(true);

        root.getChildren().addAll(header, progressBar, metaLabel);

        setText(null);
        setGraphic(root);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        setOnMouseClicked(event -> {
          SurahProgressRow row = getItem();
          if (row == null) {
            return;
          }
          openRow(row);
        });
      }
    });
  }

  private void loadReadingProgress() {
    summaryLabel.setText("Loading reading progress...");

    DbAsync.runWithUi(
        this::buildPayload,
        payload -> {
          progressListView.getItems().setAll(payload.rows());
          summaryLabel.setText(
              payload.totalSurahs() + " surahs " + '\u2022' + " " + payload.surahsWithProgress() + " with progress");
        },
        err -> {
          err.printStackTrace();
          summaryLabel.setText("Unable to load reading progress right now.");
        });
  }

  private ReadingProgressPayload buildPayload() {
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();
    List<ChapterView> chapters = quranQueryService.getAllChapters(CHAPTER_LANGUAGE);

    ReadingProgressService readingProgressService = appContext != null ? appContext.getReadingProgressService() : null;
    List<ReadingProgress> progressRows = readingProgressService != null
        ? readingProgressService.getAllProgress()
        : List.of();

    Map<Integer, ReadingProgress> latestBySurah = new HashMap<>();
    for (ReadingProgress progress : progressRows) {
      if (progress == null || progress.getSurahNumber() <= 0) {
        continue;
      }

      ReadingProgress current = latestBySurah.get(progress.getSurahNumber());
      if (current == null || isMoreRecent(progress, current)) {
        latestBySurah.put(progress.getSurahNumber(), progress);
      }
    }

    List<SurahProgressRow> rows = new ArrayList<>();
    int surahsWithProgress = 0;

    for (ChapterView chapterView : chapters) {
      if (chapterView == null || chapterView.getChapter() == null) {
        continue;
      }

      Chapter chapter = chapterView.getChapter();
      int chapterId = chapter.getId();
      int totalAyah = Math.max(0, chapter.getVerseCount());

      ReadingProgress latest = latestBySurah.get(chapterId);
      int lastReadAyah = latest != null ? clampAyah(latest.getAyahNumber(), totalAyah) : 0;
      double completionRatio = totalAyah > 0 && lastReadAyah > 0
          ? Math.min(1.0, (double) lastReadAyah / totalAyah)
          : 0.0;

      if (lastReadAyah > 0) {
        surahsWithProgress++;
      }

      rows.add(new SurahProgressRow(
          chapterView,
          chapterId,
          nonBlank(chapter.getNameSimple(), "Surah " + chapterId),
          nonBlank(chapter.getNameArabic(), ""),
          lastReadAyah,
          totalAyah,
          completionRatio,
          latest != null ? latest.getLastReadAt() : null));
    }

    return new ReadingProgressPayload(rows, rows.size(), surahsWithProgress);
  }

  private boolean isMoreRecent(ReadingProgress candidate, ReadingProgress current) {
    LocalDateTime candidateTime = candidate.getLastReadAt();
    LocalDateTime currentTime = current.getLastReadAt();

    if (candidateTime == null && currentTime == null) {
      return candidate.getAyahNumber() > current.getAyahNumber();
    }

    if (candidateTime == null) {
      return false;
    }

    if (currentTime == null) {
      return true;
    }

    if (candidateTime.isAfter(currentTime)) {
      return true;
    }

    if (candidateTime.isEqual(currentTime)) {
      return candidate.getAyahNumber() > current.getAyahNumber();
    }

    return false;
  }

  private int clampAyah(int ayahNumber, int totalAyah) {
    if (totalAyah <= 0) {
      return Math.max(0, ayahNumber);
    }
    if (ayahNumber <= 0) {
      return 0;
    }
    return Math.min(ayahNumber, totalAyah);
  }

  private String buildMetaLine(SurahProgressRow row) {
    if (row.lastReadAyah() <= 0) {
      return "Not started";
    }

    String timestampPart = row.lastReadAt() != null
        ? " " + '\u2022' + " updated " + TIME_FORMATTER.format(row.lastReadAt())
        : "";

    return "Last read ayah " + row.lastReadAyah() + " of " + row.totalAyah() + timestampPart;
  }

  private void openRow(SurahProgressRow row) {
    if (browsingController == null || row.chapterView() == null) {
      return;
    }

    int focusAyah = row.lastReadAyah() > 0 ? row.lastReadAyah() : 1;
    browsingController.showChapterAndFocusAyah(row.chapterView(), focusAyah);
  }

  private String nonBlank(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

  private record ReadingProgressPayload(List<SurahProgressRow> rows, int totalSurahs, int surahsWithProgress) {
  }

  private record SurahProgressRow(
      ChapterView chapterView,
      int chapterNumber,
      String simpleName,
      String arabicName,
      int lastReadAyah,
      int totalAyah,
      double completionRatio,
      LocalDateTime lastReadAt) {

    String percentLabel() {
      return (int) Math.round(completionRatio * 100) + "%";
    }
  }
}
