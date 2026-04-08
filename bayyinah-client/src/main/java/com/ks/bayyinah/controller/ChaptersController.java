package com.ks.bayyinah.controller;

import com.ks.bayyinah.config.ConfigManager;
import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.controller.cell.VerseCell;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.dto.TranslationView;
import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.AudioRecitation;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.model.Chapter_i18n;
import com.ks.bayyinah.core.model.Translation;
import com.ks.bayyinah.infra.hybrid.service.ReadingProgressService;
import com.ks.bayyinah.infra.hybrid.service.UserPreferenceService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.infra.media.VerseAudioPlaybackManager;
import com.ks.bayyinah.ui.ToastManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import lombok.Setter;
import org.kordamp.ikonli.javafx.FontIcon;

public class ChaptersController {

  private static final String PREF_DEFAULT_TRANSLATION = "default_translation";
  private static final String PREF_ACTIVE_RECITER = "active_reciter";
  private static final String AUDIO_SCOPE_PREFIX = "Audio";
  private static final int DEFAULT_ACTIVE_RECITER_ID = 2;
  private static final Duration PROGRESS_CAPTURE_INTERVAL = Duration.seconds(3);
  private static final long PROGRESS_HEARTBEAT_MILLIS = 15000;

  private enum AudioPlaybackState {
    IDLE,
    PLAYING,
    PAUSED
  }

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
  private ComboBox<ReciterOption> reciterSelector;

  @FXML
  private FontIcon reciterAvailabilityIcon;

  @FXML
  private Label reciterAvailabilityLabel;

  @FXML
  private Button tafsirToggleBtn;

  @FXML
  private Button audioToggleBtn;

  @FXML
  private FontIcon audioToggleIcon;

  private BrowsingController browsingController;

  @Setter
  private AppContext appContext;

  private Runnable onLoadComplete;
  private ChapterView currentChapter;
  private Integer currentStartVerse;
  private Integer currentEndVerse;
  private int selectedTranslationId = 20;
  private int selectedReciterId = DEFAULT_ACTIVE_RECITER_ID;
  private boolean ignoreTranslationSelection;
  private boolean ignoreReciterSelection;
  private boolean progressTrackingInitialized;
  private Timeline progressCaptureTimeline;
  private Integer lastSavedSurah;
  private Integer lastSavedAyah;
  private long lastSaveEpochMillis;
  private Integer pendingFocusAyah;
  private Consumer<VerseView> onVerseSyncRequested;
  private String activeAudioVerseKey;
  private int activeAudioVerseIndex = -1;
  private AudioPlaybackState audioPlaybackState = AudioPlaybackState.IDLE;
  private long audioSequenceToken;

  @FXML
  private void initialize() {
    initializeProgressTracking();
    configureReciterSelectorCellFactory();

    if (audioToggleBtn != null && !audioToggleBtn.getStyleClass().contains("reader-icon-btn")) {
      audioToggleBtn.getStyleClass().add("reader-icon-btn");
      audioToggleBtn.setText("");
    }

    updateReciterAvailabilityBadge(null);
    updateAudioToolbarState();
    applyVerseCellFactory();
  }

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  public void setOnLoadComplete(Runnable callback) {
    this.onLoadComplete = callback;
  }

  public void setOnVerseSyncRequested(Consumer<VerseView> callback) {
    this.onVerseSyncRequested = callback;
    applyVerseCellFactory();
    if (verseListView != null) {
      verseListView.refresh();
    }
  }

  public void showVerses(int chapterId) {
    showVerses(chapterId, selectedTranslationId);
  }

  public void showVerses(int chapterId, int translationId) {
    stopAudioPlayback(false);
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();

    DbAsync.runWithUi(
        () -> quranQueryService.getChapterVerses(chapterId, translationId),
        verses -> {
          verseListView.setItems(FXCollections.observableArrayList(verses));
          scrollToLastReadIfApplicable(chapterId);
          applyPendingFocusAyah();
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        },
        e -> {
          e.printStackTrace();
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        });

    applyVerseCellFactory();
  }

  public void showVerses(int chapterId, int startVerse, int endVerse) {
    showVerses(chapterId, startVerse, endVerse, selectedTranslationId);
  }

  public void showVerses(int chapterId, int startVerse, int endVerse, int translationId) {
    stopAudioPlayback(false);
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();

    DbAsync.runWithUi(
        () -> quranQueryService.getVersesByRange(chapterId, startVerse, endVerse, translationId),
        verses -> {
          verseListView.setItems(FXCollections.observableArrayList(verses));
          scrollToLastReadIfApplicable(chapterId);
          applyPendingFocusAyah();
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        },
        e -> {
          e.printStackTrace();
          if (onLoadComplete != null) {
            onLoadComplete.run();
          }
        });

    applyVerseCellFactory();
  }

  private void applyVerseCellFactory() {
    if (verseListView == null) {
      return;
    }
    verseListView.setCellFactory(
        listView -> new VerseCell(
            onVerseSyncRequested,
            this::handleVerseAudioRequest,
            this::isVerseAudioActive,
            this::isVerseAudioPaused));
  }

  public void setChapter(ChapterView chapter, Integer startVerse, Integer endVerse, Integer translationId) {
    stopAudioPlayback(false);
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
      loadChapterWithPreferences(chaptersData, startVerse, endVerse, translationId);
      return;
    }

    UserPreferenceService userPreferenceService = appContext != null ? appContext.getUserPreferenceService() : null;
    if (userPreferenceService == null) {
      loadChapterWithPreferences(chaptersData, startVerse, endVerse, selectedTranslationId);
      return;
    }

    DbAsync.runWithUi(
        userPreferenceService::getDefaultTranslation,
        preferredTranslation -> loadChapterWithPreferences(
            chaptersData,
            startVerse,
            endVerse,
            preferredTranslation != null ? preferredTranslation : selectedTranslationId),
        err -> {
          err.printStackTrace();
          loadChapterWithPreferences(chaptersData, startVerse, endVerse, selectedTranslationId);
        });
  }

  private void loadChapterWithPreferences(Chapter chapterData, Integer startVerse, Integer endVerse, int translationId) {
    resolveActiveReciterPreference(() -> loadChapterWithTranslation(chapterData, startVerse, endVerse, translationId));
  }

  public void focusAyah(Integer ayah) {
    if (ayah == null || ayah <= 0) {
      return;
    }
    pendingFocusAyah = ayah;
    applyPendingFocusAyah();
  }

  private void applyPendingFocusAyah() {
    if (pendingFocusAyah == null || pendingFocusAyah <= 0 || verseListView == null
        || verseListView.getItems() == null || verseListView.getItems().isEmpty()) {
      return;
    }

    for (int i = 0; i < verseListView.getItems().size(); i++) {
      VerseView verse = verseListView.getItems().get(i);
      if (verse != null && verse.getVerse() != null && verse.getVerse().getVerseNumber() == pendingFocusAyah) {
        verseListView.scrollTo(i);
        verseListView.getSelectionModel().clearAndSelect(i);
        verseListView.getSelectionModel().clearSelection();
        return;
      }
    }
  }

  private void loadChapterWithTranslation(Chapter chapterData, Integer startVerse, Integer endVerse,
      Integer translationId) {
    int resolvedTranslationId = translationId != null ? translationId : selectedTranslationId;
    selectedTranslationId = resolvedTranslationId;
    applyActiveReciterId(selectedReciterId);

    loadTranslationOptions(resolvedTranslationId);
    loadReciterOptions(selectedReciterId);

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
    if (audioPlaybackState == AudioPlaybackState.IDLE) {
      Optional<VerseView> playbackStart = resolvePlaybackStartVerse();
      if (playbackStart.isEmpty()) {
        ToastManager.getInstance().showInfo(AUDIO_SCOPE_PREFIX, "No verse available for playback.");
        return;
      }
      startAutoplayFromVerse(playbackStart.get(), true);
      return;
    }

    if (audioPlaybackState == AudioPlaybackState.PLAYING) {
      pauseActivePlayback();
      return;
    }

    resumeActivePlayback();
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
            UserPreferenceService userPreferenceService = appContext != null ? appContext.getUserPreferenceService() : null;
            if (userPreferenceService != null) {
              DbAsync.run(() -> userPreferenceService.setPreference(PREF_DEFAULT_TRANSLATION,
                  String.valueOf(selectedTranslationId)));
            }
            reloadCurrentChapter();
          });
        },
        err -> err.printStackTrace());
  }

  private void loadReciterOptions(int activeReciterId) {
    if (reciterSelector == null || currentChapter == null || currentChapter.getChapter() == null) {
      return;
    }

    Chapter chapter = currentChapter.getChapter();
    int chapterId = chapter.getId();
    int verseScopeTotal = resolveVerseScopeTotal(chapter.getVerseCount());

    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();
    DbAsync.runWithUi(
        () -> new ReciterOptionsPayload(
            quranQueryService.getAvailableRecitations(),
            quranQueryService.getRecitationAudioCoverageByChapter(chapterId, currentStartVerse, currentEndVerse)),
        payload -> {
          List<ReciterOption> options = new ArrayList<>();
          Map<Integer, Integer> coverageCounts = payload.coverageCountsByRecitationId();
          for (AudioRecitation recitation : payload.recitations()) {
            int availableCount = coverageCounts.getOrDefault(recitation.getId(), 0);
            options.add(new ReciterOption(
                recitation.getId(),
                buildReciterLabel(recitation),
                availableCount,
                verseScopeTotal));
          }

          if (options.isEmpty()) {
            options.add(new ReciterOption(
                activeReciterId,
                "Reciter #" + activeReciterId,
                0,
                verseScopeTotal));
          }

          ignoreReciterSelection = true;
          reciterSelector.setItems(FXCollections.observableArrayList(options));

          ReciterOption selectedOption = findReciterOptionById(options, activeReciterId)
              .orElseGet(() -> options.get(0));

          reciterSelector.getSelectionModel().select(selectedOption);
          selectedReciterId = selectedOption.id();
          applyActiveReciterId(selectedReciterId);
          updateReciterAvailabilityBadge(selectedOption);
          ignoreReciterSelection = false;

          reciterSelector.setOnAction(event -> {
            if (ignoreReciterSelection) {
              return;
            }

            ReciterOption selected = reciterSelector.getValue();
            if (selected == null || selected.id() == selectedReciterId) {
              return;
            }

            selectedReciterId = selected.id();
            applyActiveReciterId(selectedReciterId);
            persistActiveReciterPreference(selectedReciterId);
            updateReciterAvailabilityBadge(selected);

            if (!selected.hasAnyAudio()) {
              ToastManager.getInstance().showWarning(AUDIO_SCOPE_PREFIX,
                  "Selected reciter has no available audio in this chapter.");
            }

            reloadCurrentChapter();
          });
        },
        err -> {
          err.printStackTrace();
          ReciterOption fallback = new ReciterOption(
              activeReciterId,
              "Reciter #" + activeReciterId,
              0,
              resolveVerseScopeTotal(currentChapter.getChapter().getVerseCount()));
          reciterSelector.setItems(FXCollections.observableArrayList(fallback));
          reciterSelector.getSelectionModel().select(fallback);
          selectedReciterId = fallback.id();
          updateReciterAvailabilityBadge(fallback);
        });
  }

  private void configureReciterSelectorCellFactory() {
    if (reciterSelector == null) {
      return;
    }
    reciterSelector.setCellFactory(listView -> createReciterCell());
    reciterSelector.setButtonCell(createReciterCell());
  }

  private ListCell<ReciterOption> createReciterCell() {
    return new ListCell<>() {
      private final HBox root = new HBox(6);
      private final FontIcon statusIcon = new FontIcon();
      private final Label label = new Label();

      {
        statusIcon.setIconSize(14);
        statusIcon.getStyleClass().add("reciter-option-icon");
        label.getStyleClass().add("reciter-option-label");
        HBox.setHgrow(label, Priority.ALWAYS);
        root.getChildren().addAll(statusIcon, label);
      }

      @Override
      protected void updateItem(ReciterOption item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          return;
        }

        statusIcon.getStyleClass().removeAll(
            "reciter-option-icon-ok",
            "reciter-option-icon-partial",
            "reciter-option-icon-none");

        if (item.fullCoverage()) {
          statusIcon.setIconLiteral("mdi2c-check-circle-outline");
          statusIcon.getStyleClass().add("reciter-option-icon-ok");
        } else if (item.hasAnyAudio()) {
          statusIcon.setIconLiteral("mdi2a-alert-circle-outline");
          statusIcon.getStyleClass().add("reciter-option-icon-partial");
        } else {
          statusIcon.setIconLiteral("mdi2c-close-circle-outline");
          statusIcon.getStyleClass().add("reciter-option-icon-none");
        }

        label.setText(item.label() + " (" + item.availableCount() + "/" + item.totalCount() + ")");
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setText(null);
        setGraphic(root);
      }
    };
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

  private String buildReciterLabel(AudioRecitation recitation) {
    if (recitation == null) {
      return "Unknown reciter";
    }

    String primaryLabel = recitation.getTranslatedName();
    if (primaryLabel == null || primaryLabel.isBlank()) {
      primaryLabel = recitation.getReciterName();
    }
    if (primaryLabel == null || primaryLabel.isBlank()) {
      primaryLabel = "Reciter #" + recitation.getId();
    }

    String style = recitation.getStyle();
    if (style == null || style.isBlank()) {
      return primaryLabel;
    }

    return primaryLabel + " - " + style;
  }

  private void reloadCurrentChapter() {
    if (currentChapter == null) {
      return;
    }
    stopAudioPlayback(false);
    persistVisibleProgressNow();
    Chapter chapterData = currentChapter.getChapter();
    if (currentStartVerse == null || currentEndVerse == null) {
      showVerses(chapterData.getId(), selectedTranslationId);
    } else {
      showVerses(chapterData.getId(), currentStartVerse, currentEndVerse, selectedTranslationId);
    }
  }

  private void handleVerseAudioRequest(VerseView verseView) {
    if (verseView == null || verseView.getVerse() == null) {
      return;
    }

    String verseKey = verseView.getVerse().getVerseKey();
    if (verseKey == null || verseKey.isBlank()) {
      ToastManager.getInstance().showWarning(AUDIO_SCOPE_PREFIX, "Selected verse cannot be played.");
      return;
    }

    if (verseKey.equals(activeAudioVerseKey)) {
      if (audioPlaybackState == AudioPlaybackState.PLAYING) {
        pauseActivePlayback();
        return;
      }
      if (audioPlaybackState == AudioPlaybackState.PAUSED) {
        resumeActivePlayback();
        return;
      }
    }

    startAutoplayFromVerse(verseView, true);
  }

  private void startAutoplayFromVerse(VerseView verseView, boolean userInitiated) {
    if (verseListView == null || verseListView.getItems() == null || verseListView.getItems().isEmpty()) {
      ToastManager.getInstance().showInfo(AUDIO_SCOPE_PREFIX, "No verses available in this chapter.");
      return;
    }

    VerseAudioPlaybackManager playbackManager = resolvePlaybackManager(true);
    if (playbackManager == null) {
      return;
    }

    int requestedIndex = findVerseIndex(verseView);
    if (requestedIndex < 0) {
      requestedIndex = 0;
    }

    int playableIndex = findNextPlayableVerseIndex(requestedIndex);
    if (playableIndex < 0) {
      stopAudioPlayback(true);
      ToastManager.getInstance().showWarning(AUDIO_SCOPE_PREFIX,
          "No playable local audio files found in this chapter for the selected reciter.");
      return;
    }

    if (userInitiated && playableIndex != requestedIndex) {
      VerseView fallbackVerse = verseListView.getItems().get(playableIndex);
      String fallbackKey = fallbackVerse != null && fallbackVerse.getVerse() != null
          ? fallbackVerse.getVerse().getVerseKey()
          : "the next available verse";
      ToastManager.getInstance().showInfo(
          AUDIO_SCOPE_PREFIX,
          "Selected verse has no local audio. Starting from " + fallbackKey + ".");
    }

    playbackManager.stop();
    long sequenceToken = invalidateAudioSequence();
    playVerseAtIndex(playableIndex, sequenceToken);
  }

  private void playVerseAtIndex(int verseIndex, long sequenceToken) {
    if (!isAudioSequenceCurrent(sequenceToken)
        || verseListView == null
        || verseListView.getItems() == null
        || verseIndex < 0
        || verseIndex >= verseListView.getItems().size()) {
      return;
    }

    VerseView verseView = verseListView.getItems().get(verseIndex);
    if (verseView == null || verseView.getVerse() == null) {
      completeAutoplay(sequenceToken);
      return;
    }

    Path resolvedPath = resolveAudioFilePath(verseView);
    if (resolvedPath == null || !Files.exists(resolvedPath)) {
      int nextPlayable = findNextPlayableVerseIndex(verseIndex + 1);
      if (nextPlayable < 0) {
        completeAutoplay(sequenceToken);
      } else {
        playVerseAtIndex(nextPlayable, sequenceToken);
      }
      return;
    }

    VerseAudioPlaybackManager playbackManager = resolvePlaybackManager(true);
    if (playbackManager == null) {
      stopAudioPlayback(true);
      return;
    }

    activeAudioVerseIndex = verseIndex;
    activeAudioVerseKey = verseView.getVerse().getVerseKey();
    audioPlaybackState = AudioPlaybackState.PLAYING;
    refreshAudioUi(true);

    playbackManager.play(
        resolvedPath,
        activeAudioVerseKey,
        () -> {
          if (!isAudioSequenceCurrent(sequenceToken)) {
            return;
          }

          int nextPlayable = findNextPlayableVerseIndex(verseIndex + 1);
          if (nextPlayable < 0) {
            completeAutoplay(sequenceToken);
            return;
          }

          playVerseAtIndex(nextPlayable, sequenceToken);
        },
        error -> {
          if (!isAudioSequenceCurrent(sequenceToken)) {
            return;
          }

          ToastManager.getInstance().showError(AUDIO_SCOPE_PREFIX, error);
          int nextPlayable = findNextPlayableVerseIndex(verseIndex + 1);
          if (nextPlayable < 0) {
            completeAutoplay(sequenceToken);
            return;
          }

          playVerseAtIndex(nextPlayable, sequenceToken);
        });
  }

  private void completeAutoplay(long sequenceToken) {
    if (!isAudioSequenceCurrent(sequenceToken)) {
      return;
    }

    stopAudioPlayback(true);
    ToastManager.getInstance().showInfo(AUDIO_SCOPE_PREFIX, "Reached the end of the selected verses.");
  }

  private void pauseActivePlayback() {
    VerseAudioPlaybackManager playbackManager = resolvePlaybackManager(true);
    if (playbackManager == null || audioPlaybackState != AudioPlaybackState.PLAYING) {
      return;
    }

    boolean paused = playbackManager.pause();
    if (!paused) {
      ToastManager.getInstance().showWarning(AUDIO_SCOPE_PREFIX, "Unable to pause verse playback.");
      return;
    }

    audioPlaybackState = AudioPlaybackState.PAUSED;
    refreshAudioUi(false);
  }

  private void resumeActivePlayback() {
    VerseAudioPlaybackManager playbackManager = resolvePlaybackManager(true);
    if (playbackManager == null || audioPlaybackState != AudioPlaybackState.PAUSED) {
      return;
    }

    boolean resumed = playbackManager.resume();
    if (!resumed) {
      ToastManager.getInstance().showWarning(AUDIO_SCOPE_PREFIX, "Unable to resume verse playback.");
      return;
    }

    audioPlaybackState = AudioPlaybackState.PLAYING;
    refreshAudioUi(false);
  }

  private VerseAudioPlaybackManager resolvePlaybackManager(boolean showError) {
    VerseAudioPlaybackManager playbackManager = appContext != null ? appContext.getVerseAudioPlaybackManager() : null;
    if (playbackManager == null && showError) {
      ToastManager.getInstance().showError(AUDIO_SCOPE_PREFIX, "Audio playback is not available.");
    }
    return playbackManager;
  }

  private Optional<VerseView> resolvePlaybackStartVerse() {
    if (verseListView == null || verseListView.getItems() == null || verseListView.getItems().isEmpty()) {
      return Optional.empty();
    }

    VerseView selected = verseListView.getSelectionModel().getSelectedItem();
    if (selected != null && selected.getVerse() != null) {
      return Optional.of(selected);
    }

    return resolveCurrentVisibleVerse();
  }

  private int findVerseIndex(VerseView verseView) {
    if (verseView == null || verseView.getVerse() == null || verseListView == null || verseListView.getItems() == null) {
      return -1;
    }

    String verseKey = verseView.getVerse().getVerseKey();
    if (verseKey == null || verseKey.isBlank()) {
      return -1;
    }

    for (int i = 0; i < verseListView.getItems().size(); i++) {
      VerseView item = verseListView.getItems().get(i);
      if (item != null && item.getVerse() != null && verseKey.equals(item.getVerse().getVerseKey())) {
        return i;
      }
    }
    return -1;
  }

  private int findNextPlayableVerseIndex(int startInclusiveIndex) {
    if (verseListView == null || verseListView.getItems() == null || verseListView.getItems().isEmpty()) {
      return -1;
    }

    int start = Math.max(0, startInclusiveIndex);
    for (int i = start; i < verseListView.getItems().size(); i++) {
      VerseView candidate = verseListView.getItems().get(i);
      Path path = resolveAudioFilePath(candidate);
      if (path != null && Files.exists(path)) {
        return i;
      }
    }
    return -1;
  }

  private Path resolveAudioFilePath(VerseView verseView) {
    if (verseView == null || verseView.getAudioLocalPath() == null || verseView.getAudioLocalPath().isBlank()) {
      return null;
    }

    try {
      String audioRootPath = resolveAudioRootPath();
      if (audioRootPath == null || audioRootPath.isBlank()) {
        return null;
      }

      Path audioRoot = Path.of(audioRootPath).toAbsolutePath().normalize();
      Path localPath = Path.of(verseView.getAudioLocalPath());
      if (localPath.isAbsolute()) {
        return null;
      }

      Path resolvedPath = audioRoot.resolve(localPath).normalize();
      if (!resolvedPath.startsWith(audioRoot)) {
        return null;
      }

      return resolvedPath;
    } catch (Exception ignored) {
      return null;
    }
  }

  private String resolveAudioRootPath() {
    if (appContext == null || appContext.getMainConfig() == null || appContext.getMainConfig().getAudio() == null) {
      return null;
    }
    return appContext.getMainConfig().getAudio().getAudioRootPath();
  }

  private boolean isVerseAudioActive(VerseView verseView) {
    if (audioPlaybackState == AudioPlaybackState.IDLE
        || verseView == null
        || verseView.getVerse() == null
        || activeAudioVerseKey == null) {
      return false;
    }
    return activeAudioVerseKey.equals(verseView.getVerse().getVerseKey());
  }

  private boolean isVerseAudioPaused(VerseView verseView) {
    return isVerseAudioActive(verseView) && audioPlaybackState == AudioPlaybackState.PAUSED;
  }

  private void stopAudioPlayback(boolean refreshVerseList) {
    VerseAudioPlaybackManager playbackManager = appContext != null ? appContext.getVerseAudioPlaybackManager() : null;
    if (playbackManager != null) {
      playbackManager.stop();
    }

    invalidateAudioSequence();

    activeAudioVerseKey = null;
    activeAudioVerseIndex = -1;
    audioPlaybackState = AudioPlaybackState.IDLE;
    updateAudioToolbarState();

    if (refreshVerseList && verseListView != null) {
      verseListView.refresh();
    }
  }

  private long invalidateAudioSequence() {
    audioSequenceToken += 1;
    return audioSequenceToken;
  }

  private boolean isAudioSequenceCurrent(long sequenceToken) {
    return sequenceToken == audioSequenceToken;
  }

  private void refreshAudioUi(boolean scrollToActiveVerse) {
    updateAudioToolbarState();
    if (scrollToActiveVerse) {
      ensureActiveVerseInView();
    }
    if (verseListView != null) {
      verseListView.refresh();
    }
  }

  private void ensureActiveVerseInView() {
    if (verseListView == null || activeAudioVerseIndex < 0 || activeAudioVerseIndex >= verseListView.getItems().size()) {
      return;
    }
    verseListView.scrollTo(activeAudioVerseIndex);
  }

  private void updateAudioToolbarState() {
    if (audioToggleBtn == null) {
      return;
    }

    if (!audioToggleBtn.getStyleClass().contains("reader-icon-btn")) {
      audioToggleBtn.getStyleClass().add("reader-icon-btn");
    }
    audioToggleBtn.getStyleClass().removeAll("is-idle", "is-playing", "is-paused");

    String iconLiteral = "mdi2p-play";
    String tooltip = "Play recitation from the selected verse";
    String stateClass = "is-idle";

    if (audioPlaybackState == AudioPlaybackState.PLAYING) {
      iconLiteral = "mdi2p-pause";
      tooltip = "Pause recitation";
      stateClass = "is-playing";
    } else if (audioPlaybackState == AudioPlaybackState.PAUSED) {
      iconLiteral = "mdi2p-play";
      tooltip = "Resume recitation";
      stateClass = "is-paused";
    }

    audioToggleBtn.getStyleClass().add(stateClass);
    audioToggleBtn.setText("");
    audioToggleBtn.setTooltip(new Tooltip(tooltip));

    if (audioToggleIcon != null) {
      audioToggleIcon.setIconLiteral(iconLiteral);
      audioToggleIcon.setIconSize(16);
    }
  }

  private void resolveActiveReciterPreference(Runnable onResolved) {
    UserPreferenceService userPreferenceService = appContext != null ? appContext.getUserPreferenceService() : null;
    if (userPreferenceService == null) {
      selectedReciterId = resolveConfiguredReciterId();
      applyActiveReciterId(selectedReciterId);
      if (onResolved != null) {
        onResolved.run();
      }
      return;
    }

    DbAsync.runWithUi(
        () -> parseIntPreference(userPreferenceService.getPreference(PREF_ACTIVE_RECITER) != null
            ? userPreferenceService.getPreference(PREF_ACTIVE_RECITER).getValue()
            : null),
        preferredReciterId -> {
          if (preferredReciterId != null && preferredReciterId > 0) {
            selectedReciterId = preferredReciterId;
          } else {
            selectedReciterId = resolveConfiguredReciterId();
          }
          applyActiveReciterId(selectedReciterId);
          if (onResolved != null) {
            onResolved.run();
          }
        },
        err -> {
          err.printStackTrace();
          selectedReciterId = resolveConfiguredReciterId();
          applyActiveReciterId(selectedReciterId);
          if (onResolved != null) {
            onResolved.run();
          }
        });
  }

  private void persistActiveReciterPreference(int reciterId) {
    UserPreferenceService userPreferenceService = appContext != null ? appContext.getUserPreferenceService() : null;
    if (userPreferenceService == null || reciterId <= 0) {
      return;
    }

    DbAsync.run(() -> userPreferenceService.setPreference(PREF_ACTIVE_RECITER, String.valueOf(reciterId)));
  }

  private int resolveConfiguredReciterId() {
    if (appContext != null
        && appContext.getMainConfig() != null
        && appContext.getMainConfig().getAudio() != null
        && appContext.getMainConfig().getAudio().getActiveReciterId() != null
        && appContext.getMainConfig().getAudio().getActiveReciterId() > 0) {
      return appContext.getMainConfig().getAudio().getActiveReciterId();
    }
    return DEFAULT_ACTIVE_RECITER_ID;
  }

  private void applyActiveReciterId(int reciterId) {
    if (reciterId <= 0) {
      return;
    }

    if (appContext != null && appContext.getMainConfig() != null && appContext.getMainConfig().getAudio() != null) {
      appContext.getMainConfig().getAudio().setActiveReciterId(reciterId);
    }

    try {
      if (ConfigManager.getConfig() != null
          && ConfigManager.getConfig().getAudio() != null) {
        ConfigManager.getConfig().getAudio().setActiveReciterId(reciterId);
      }
    } catch (Exception ignored) {
      // Keep runtime behavior even if config singleton cannot be updated.
    }
  }

  private int resolveVerseScopeTotal(int chapterVerseCount) {
    if (currentStartVerse != null && currentEndVerse != null && currentEndVerse >= currentStartVerse) {
      return (currentEndVerse - currentStartVerse) + 1;
    }
    return Math.max(chapterVerseCount, 0);
  }

  private Optional<ReciterOption> findReciterOptionById(List<ReciterOption> options, int reciterId) {
    if (options == null || options.isEmpty()) {
      return Optional.empty();
    }
    for (ReciterOption option : options) {
      if (option.id() == reciterId) {
        return Optional.of(option);
      }
    }
    return Optional.empty();
  }

  private void updateReciterAvailabilityBadge(ReciterOption option) {
    if (reciterAvailabilityIcon == null || reciterAvailabilityLabel == null) {
      return;
    }

    reciterAvailabilityIcon.getStyleClass().removeAll(
        "reciter-availability-ok",
        "reciter-availability-partial",
        "reciter-availability-none");

    if (option == null) {
      reciterAvailabilityLabel.setText("Audio 0/0");
      reciterAvailabilityIcon.setIconLiteral("mdi2c-close-circle-outline");
      reciterAvailabilityIcon.getStyleClass().add("reciter-availability-none");
      return;
    }

    reciterAvailabilityLabel.setText("Audio " + option.availableCount() + "/" + option.totalCount());
    if (option.fullCoverage()) {
      reciterAvailabilityIcon.setIconLiteral("mdi2c-check-circle-outline");
      reciterAvailabilityIcon.getStyleClass().add("reciter-availability-ok");
    } else if (option.hasAnyAudio()) {
      reciterAvailabilityIcon.setIconLiteral("mdi2a-alert-circle-outline");
      reciterAvailabilityIcon.getStyleClass().add("reciter-availability-partial");
    } else {
      reciterAvailabilityIcon.setIconLiteral("mdi2c-close-circle-outline");
      reciterAvailabilityIcon.getStyleClass().add("reciter-availability-none");
    }

    reciterAvailabilityLabel.setTooltip(new Tooltip(
        option.hasAnyAudio()
            ? "Selected reciter has audio for " + option.availableCount() + " of " + option.totalCount() + " verses."
            : "Selected reciter has no local audio in this chapter."));
  }

  private record TranslationOption(int id, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private record TranslationOptionsPayload(List<TranslationView> translations, Set<Integer> availableTranslationIds) {
  }

  private record ReciterOption(int id, String label, int availableCount, int totalCount) {
    private boolean fullCoverage() {
      return totalCount > 0 && availableCount >= totalCount;
    }

    private boolean hasAnyAudio() {
      return availableCount > 0;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private record ReciterOptionsPayload(List<AudioRecitation> recitations, Map<Integer, Integer> coverageCountsByRecitationId) {
  }

  private void initializeProgressTracking() {
    if (progressTrackingInitialized || verseListView == null) {
      return;
    }
    progressTrackingInitialized = true;

    progressCaptureTimeline = new Timeline(
        new KeyFrame(PROGRESS_CAPTURE_INTERVAL, event -> persistVisibleProgressIfNeeded()));
    progressCaptureTimeline.setCycleCount(Timeline.INDEFINITE);
    progressCaptureTimeline.play();

    verseListView.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene == null && progressCaptureTimeline != null) {
        progressCaptureTimeline.stop();
        stopAudioPlayback(false);
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
    },
        savedAyah -> {
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
        },
        err -> err.printStackTrace());
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
