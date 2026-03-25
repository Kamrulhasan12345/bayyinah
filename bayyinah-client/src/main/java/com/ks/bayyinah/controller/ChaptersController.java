package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.controller.cell.VerseCell;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.dto.TranslationView;
import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.model.Chapter_i18n;
import com.ks.bayyinah.core.model.Translation;
import com.ks.bayyinah.infra.hybrid.service.ReadingProgressService;
import com.ks.bayyinah.infra.hybrid.service.UserPreferenceService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.ui.ToastManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Set;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import lombok.Setter;
import javafx.util.Duration;

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
  private Label chapterMeta;

  @FXML
  private ListView<VerseView> verseListView;

  @FXML
  private ComboBox<TranslationOption> translationSelector;

  @FXML
  private Button tafsirToggleBtn;

  @FXML
  private Button audioToggleBtn;

  private BrowsingController browsingController;

  @Setter
  private AppContext appContext;

  private Runnable onLoadComplete;
  private ChapterView currentChapter;
  private Integer currentStartVerse;
  private Integer currentEndVerse;
  private int selectedTranslationId = 20;
  private boolean ignoreTranslationSelection;
  private boolean progressTrackingInitialized;
  private Timeline progressCaptureTimeline;
  private Integer lastSavedSurah;
  private Integer lastSavedAyah;
  private long lastSaveEpochMillis;

  private static final Duration PROGRESS_CAPTURE_INTERVAL = Duration.seconds(3);
  private static final long PROGRESS_HEARTBEAT_MILLIS = 15000;

  @FXML
  private void initialize() {
    initializeProgressTracking();
  }

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  public void setOnLoadComplete(Runnable callback) {
    this.onLoadComplete = callback;
  }

  public void showVerses(int chapterId) {
    showVerses(chapterId, selectedTranslationId);
  }

  public void showVerses(int chapterId, int translationId) {
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();

    DbAsync.runWithUi(
        () -> quranQueryService.getChapterVerses(chapterId, translationId), verses -> {
          System.out.println("Fetched " + verses.size() + " verses for chapter " + chapterId);
          verseListView.setItems(FXCollections.observableArrayList(verses));
          scrollToLastReadIfApplicable(chapterId);
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
    showVerses(chapterId, startVerse, endVerse, selectedTranslationId);
  }

  public void showVerses(int chapterId, int startVerse, int endVerse, int translationId) {
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();

    DbAsync.runWithUi(
        () -> quranQueryService.getVersesByRange(chapterId, startVerse, endVerse,
            translationId),
        verses -> {
          System.out.println("Fetched " + verses.size() + " verses for chapter " + chapterId);
          verseListView.setItems(FXCollections.observableArrayList(verses));
          scrollToLastReadIfApplicable(chapterId);
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
    this.currentChapter = chapter;
    this.currentStartVerse = startVerse;
    this.currentEndVerse = endVerse;
    initializeProgressTracking();

    Chapter chaptersData = chapter.getChapter();
    Chapter_i18n chapterI18n = chapter.getChapterI18N();
    nameArabic.setText(chaptersData.getNameArabic());
    nameSimple.setText(chaptersData.getNameSimple());
    chapterMeta.setText(chaptersData.getVerseCount() + " ayahs • " + chaptersData.getRevelationPlace());

    String translated = "";
    if (chapterI18n != null && chapterI18n.getTranslatedName() != null) {
      translated = chapterI18n.getTranslatedName();
    } else if (chaptersData.getNameSimple() != null) {
      translated = chaptersData.getNameSimple();
    }
    translatedName.setText(translated);

    if (translationId != null) {
      loadChapterWithTranslation(chaptersData, startVerse, endVerse, translationId);
      return;
    }

    UserPreferenceService userPreferenceService = appContext.getUserPreferenceService();
    DbAsync.runWithUi(userPreferenceService::getDefaultTranslation,
        preferredTranslation -> loadChapterWithTranslation(chaptersData, startVerse, endVerse, preferredTranslation),
        err -> {
          err.printStackTrace();
          loadChapterWithTranslation(chaptersData, startVerse, endVerse, selectedTranslationId);
        });
  }

  private void loadChapterWithTranslation(Chapter chapterData, Integer startVerse, Integer endVerse, Integer translationId) {
    int resolvedTranslationId = translationId != null ? translationId : selectedTranslationId;
    selectedTranslationId = resolvedTranslationId;
    loadTranslationOptions(resolvedTranslationId);

    if (startVerse == null || endVerse == null) {
      showVerses(chapterData.getId(), resolvedTranslationId);
    } else {
      showVerses(chapterData.getId(), startVerse, endVerse, resolvedTranslationId);
    }
  }

  @FXML
  private void onToggleTafsir() {
    ToastManager.getInstance().showInfo("Tafsir", "Tafsir panel will be added in the next step.");
  }

  @FXML
  private void onToggleAudio() {
    ToastManager.getInstance().showInfo("Audio", "Audio controls will be added in the next step.");
  }

  private void loadTranslationOptions(int activeTranslationId) {
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();

    DbAsync.runWithUi(
        () -> new TranslationOptionsPayload(
            quranQueryService.getAvailableTranslations(),
            quranQueryService.getTranslationIdsWithAvailableText()),
        payload -> {
          List<TranslationView> translations = payload.translations();
          Set<Integer> availableIds = payload.availableTranslationIds();

          List<TranslationOption> options = new ArrayList<>();
          for (TranslationView translation : translations) {
            options.add(new TranslationOption(
                translation.getId(),
                buildTranslationLabel(translation, availableIds.contains(translation.getId()))));
          }

          if (options.isEmpty()) {
            options.add(new TranslationOption(activeTranslationId, "Translation " + activeTranslationId));
          }

          ignoreTranslationSelection = true;
          translationSelector.setItems(FXCollections.observableArrayList(options));

          TranslationOption match = null;
          for (TranslationOption option : options) {
            if (option.id() == activeTranslationId) {
              match = option;
              break;
            }
          }

          if (match == null) {
            match = options.get(0);
          }

          translationSelector.getSelectionModel().select(match);
          selectedTranslationId = match.id();
          ignoreTranslationSelection = false;

          translationSelector.setOnAction(event -> {
            if (ignoreTranslationSelection) {
              return;
            }
            TranslationOption selected = translationSelector.getValue();
            if (selected == null || selected.id() == selectedTranslationId) {
              return;
            }

            selectedTranslationId = selected.id();
            UserPreferenceService userPreferenceService = appContext.getUserPreferenceService();
            DbAsync.run(() -> userPreferenceService.setPreference("default_translation", String.valueOf(selectedTranslationId)));
            reloadCurrentChapter();
          });
        },
        err -> err.printStackTrace());
  }

  private String buildTranslationLabel(TranslationView translationView, boolean available) {
    if (translationView == null) {
      return "Unknown - unknown (unavailable)";
    }

    Translation translation = translationView.getTranslation();
    String authorName = translation != null ? translation.getAuthorName() : null;
    String language = translation != null ? translation.getLanguage() : null;

    if (authorName == null || authorName.isBlank()) {
      authorName = "Translation #" + translationView.getId();
    }
    if (language == null || language.isBlank()) {
      language = "english";
    }

    String status = available ? "available" : "unavailable";
    return authorName + " - " + language + " (" + status + ")";
  }

  private void reloadCurrentChapter() {
    if (currentChapter == null) {
      return;
    }
    persistVisibleProgressNow();
    Chapter chapterData = currentChapter.getChapter();
    if (currentStartVerse == null || currentEndVerse == null) {
      showVerses(chapterData.getId(), selectedTranslationId);
    } else {
      showVerses(chapterData.getId(), currentStartVerse, currentEndVerse, selectedTranslationId);
    }
  }

  private record TranslationOption(int id, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private record TranslationOptionsPayload(List<TranslationView> translations, Set<Integer> availableTranslationIds) {
  }

  private void initializeProgressTracking() {
    if (progressTrackingInitialized || verseListView == null) {
      return;
    }
    progressTrackingInitialized = true;

    progressCaptureTimeline = new Timeline(new KeyFrame(PROGRESS_CAPTURE_INTERVAL, event -> persistVisibleProgressIfNeeded()));
    progressCaptureTimeline.setCycleCount(Timeline.INDEFINITE);
    progressCaptureTimeline.play();

    verseListView.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene == null && progressCaptureTimeline != null) {
        progressCaptureTimeline.stop();
      } else if (newScene != null && progressCaptureTimeline != null) {
        progressCaptureTimeline.play();
      }
    });
  }

  private void persistVisibleProgressIfNeeded() {
    Optional<VerseView> candidate = resolveCurrentVisibleVerse();
    if (candidate.isEmpty()) {
      return;
    }

    VerseView verseView = candidate.get();
    int surah = verseView.getVerse().getSurahId();
    int ayah = verseView.getVerse().getVerseNumber();
    long now = System.currentTimeMillis();

    boolean sameAsLast = lastSavedSurah != null && lastSavedAyah != null
        && lastSavedSurah == surah && lastSavedAyah == ayah;
    if (sameAsLast && now - lastSaveEpochMillis < PROGRESS_HEARTBEAT_MILLIS) {
      return;
    }

    persistProgressAsync(surah, ayah);
  }

  private void persistVisibleProgressNow() {
    Optional<VerseView> candidate = resolveCurrentVisibleVerse();
    candidate.ifPresent(verseView -> persistProgressAsync(
        verseView.getVerse().getSurahId(),
        verseView.getVerse().getVerseNumber()));
  }

  private void persistProgressAsync(int surah, int ayah) {
    if (appContext == null) {
      return;
    }
    ReadingProgressService readingProgressService = appContext.getReadingProgressService();
    UserPreferenceService userPreferenceService = appContext.getUserPreferenceService();
    if (readingProgressService == null) {
      return;
    }

    lastSavedSurah = surah;
    lastSavedAyah = ayah;
    lastSaveEpochMillis = System.currentTimeMillis();

    DbAsync.run(() -> {
      readingProgressService.recordProgress(surah, ayah);
      if (userPreferenceService != null) {
        userPreferenceService.setPreference("last_read_surah", String.valueOf(surah));
        userPreferenceService.setPreference("last_read_ayah", String.valueOf(ayah));
      }
    });
  }

  private void scrollToLastReadIfApplicable(int chapterId) {
    if (appContext == null) {
      return;
    }

    UserPreferenceService userPreferenceService = appContext.getUserPreferenceService();
    if (userPreferenceService == null) {
      return;
    }

    DbAsync.runWithUi(() -> {
      Integer savedSurah = parseIntPreference(userPreferenceService.getPreference("last_read_surah") != null
          ? userPreferenceService.getPreference("last_read_surah").getValue()
          : null);
      Integer savedAyah = parseIntPreference(userPreferenceService.getPreference("last_read_ayah") != null
          ? userPreferenceService.getPreference("last_read_ayah").getValue()
          : null);

      if (savedSurah == null || savedAyah == null || savedSurah != chapterId) {
        return null;
      }
      return savedAyah;
    }, savedAyah -> {
      if (savedAyah == null || verseListView.getItems() == null || verseListView.getItems().isEmpty()) {
        return;
      }

      for (int i = 0; i < verseListView.getItems().size(); i++) {
        VerseView verse = verseListView.getItems().get(i);
        if (verse != null && verse.getVerse() != null && verse.getVerse().getVerseNumber() == savedAyah) {
          verseListView.scrollTo(i);
          verseListView.getSelectionModel().clearSelection();
          return;
        }
      }
    }, err -> err.printStackTrace());
  }

  private Integer parseIntPreference(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private Optional<VerseView> resolveCurrentVisibleVerse() {
    if (verseListView == null || verseListView.getItems() == null || verseListView.getItems().isEmpty()) {
      return Optional.empty();
    }

    return verseListView.lookupAll(".list-cell")
        .stream()
        .filter(node -> node instanceof ListCell<?> cell && !cell.isEmpty() && cell.getItem() instanceof VerseView)
        .map(node -> (ListCell<VerseView>) node)
        .filter(ListCell::isVisible)
        .min(Comparator.comparingDouble(this::safeTopY))
        .map(ListCell::getItem)
        .or(() -> Optional.ofNullable(verseListView.getItems().get(0)));
  }

  private double safeTopY(Node node) {
    try {
      return node.localToScene(node.getBoundsInLocal()).getMinY();
    } catch (Exception ignored) {
      return Double.MAX_VALUE;
    }
  }
}
