package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.infra.hybrid.model.Bookmark;
import com.ks.bayyinah.infra.hybrid.model.ReadingProgress;
import com.ks.bayyinah.infra.hybrid.service.BookmarkService;
import com.ks.bayyinah.infra.hybrid.service.ReadingProgressService;
import com.ks.bayyinah.infra.hybrid.service.UserPreferenceService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.ui.ToastManager;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Setter;

public class HomeController {

  private static final String CHAPTER_LANGUAGE = "en";
  private static final int FALLBACK_TRANSLATION_ID = 20;
  private static final int MAX_BOOKMARKS = 5;
  private static final int MAX_QUICK_SURAHS = 8;
  private static final int[] PRIORITY_SURAHS = { 1, 2, 3, 18, 36, 55, 67, 112 };
  private static final DateTimeFormatter READ_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm");

  @FXML
  private VBox homeContainer;

  @FXML
  private TextField searchFieldHero;

  @FXML
  private Button searchButtonHero;

  @FXML
  private Label continueReadingSummaryLabel;

  @FXML
  private VBox continueReadingContainer;

  @FXML
  private Label bookmarksSummaryLabel;

  @FXML
  private FlowPane recentBookmarksPane;

  @FXML
  private Label quickSurahSummaryLabel;

  @FXML
  private FlowPane quickSurahPane;

  private BrowsingController browsingController;

  @Setter
  private AppContext appContext;

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
    this.appContext = browsingController.getAppContext();
  }

  public void initializeHome() {
    searchFieldHero.setOnAction(event -> onSearchClicked());
    loadHomeData();
  }

  @FXML
  private void onSearchClicked() {
    String query = searchFieldHero.getText() != null ? searchFieldHero.getText().trim() : "";

    if (query.isBlank()) {
      ToastManager.getInstance().showWarning("Search", "Please enter a search query.");
      return;
    }

    if (browsingController == null) {
      ToastManager.getInstance().showWarning("Search", "Search is not available right now.");
      return;
    }

    browsingController.showSearchResults(query);
  }

  private void loadHomeData() {
    continueReadingSummaryLabel.setText("Loading your latest progress...");
    bookmarksSummaryLabel.setText("Loading bookmarks...");
    quickSurahSummaryLabel.setText("Loading shortcuts...");

    continueReadingContainer.getChildren().clear();
    recentBookmarksPane.getChildren().clear();
    quickSurahPane.getChildren().clear();

    if (appContext == null) {
      renderLoadFailure("Home data is unavailable right now.");
      return;
    }

    DbAsync.runWithUi(
        this::buildPayload,
        this::renderPayload,
        error -> {
          error.printStackTrace();
          renderLoadFailure("Unable to load homepage data right now.");
        });
  }

  private HomePayload buildPayload() {
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();
    List<ChapterView> chapters = quranQueryService.getAllChapters(CHAPTER_LANGUAGE);
    Map<Integer, ChapterView> chaptersById = indexChaptersById(chapters);

    UserPreferenceService userPreferenceService = appContext.getUserPreferenceService();
    ReadingProgressService readingProgressService = appContext.getReadingProgressService();
    BookmarkService bookmarkService = appContext.getBookmarkService();

    int translationId = resolveTranslationId(userPreferenceService);

    ContinueReadingItem continueReading = buildContinueReadingItem(readingProgressService, quranQueryService,
        chaptersById, translationId);
    List<BookmarkItem> bookmarks = buildBookmarkItems(bookmarkService, quranQueryService, chaptersById, translationId);
    List<QuickSurahItem> quickSurahs = buildQuickSurahItems(chapters, chaptersById);

    return new HomePayload(continueReading, bookmarks, quickSurahs);
  }

  private ContinueReadingItem buildContinueReadingItem(
      ReadingProgressService readingProgressService,
      LocalQuranQueryService quranQueryService,
      Map<Integer, ChapterView> chaptersById,
      int translationId) {
    if (readingProgressService == null) {
      return null;
    }

    Optional<ReadingProgress> latestOpt = readingProgressService.getLatestProgress();
    if (latestOpt.isEmpty()) {
      return null;
    }

    ReadingProgress latest = latestOpt.get();
    int surahNumber = latest.getSurahNumber();
    int ayahNumber = latest.getAyahNumber();
    ChapterView chapterView = chaptersById.get(surahNumber);

    String verseKey = surahNumber + ":" + ayahNumber;
    Optional<VerseView> verseOpt = quranQueryService.getVerse(verseKey, translationId);

    String arabic = verseOpt
        .map(VerseView::getArabicText)
        .filter(text -> text != null && !text.isBlank())
        .orElse("Arabic text unavailable.");

    String translation = verseOpt
        .map(VerseView::getTranslatedText)
        .filter(text -> text != null && !text.isBlank())
        .orElse("Translation unavailable.");

    String updatedLabel = latest.getLastReadAt() != null
        ? "Updated " + READ_TIME_FORMATTER.format(latest.getLastReadAt())
        : "Recently read";

    return new ContinueReadingItem(
        chapterView,
        surahNumber,
        ayahNumber,
        buildChapterLabel(chapterView, surahNumber),
        arabic,
        translation,
        updatedLabel);
  }

  private List<BookmarkItem> buildBookmarkItems(
      BookmarkService bookmarkService,
      LocalQuranQueryService quranQueryService,
      Map<Integer, ChapterView> chaptersById,
      int translationId) {
    if (bookmarkService == null) {
      return List.of();
    }

    List<BookmarkItem> items = new ArrayList<>();
    for (Bookmark bookmark : bookmarkService.getAll()) {
      if (bookmark == null || bookmark.getSurahNumber() <= 0 || bookmark.getAyahNumber() <= 0) {
        continue;
      }

      if (items.size() >= MAX_BOOKMARKS) {
        break;
      }

      int surahNumber = bookmark.getSurahNumber();
      int ayahNumber = bookmark.getAyahNumber();
      ChapterView chapterView = chaptersById.get(surahNumber);

      String verseKey = surahNumber + ":" + ayahNumber;
      String arabic = quranQueryService.getVerse(verseKey, translationId)
          .map(VerseView::getArabicText)
          .filter(text -> text != null && !text.isBlank())
          .orElse("Arabic text unavailable.");

      items.add(new BookmarkItem(
          chapterView,
          surahNumber,
          ayahNumber,
          buildChapterLabel(chapterView, surahNumber),
          arabic));
    }

    return items;
  }

  private List<QuickSurahItem> buildQuickSurahItems(List<ChapterView> chapters, Map<Integer, ChapterView> chaptersById) {
    LinkedHashSet<Integer> selectedIds = new LinkedHashSet<>();

    for (int chapterId : PRIORITY_SURAHS) {
      if (chaptersById.containsKey(chapterId)) {
        selectedIds.add(chapterId);
      }
    }

    for (ChapterView chapterView : chapters) {
      if (selectedIds.size() >= MAX_QUICK_SURAHS) {
        break;
      }

      if (chapterView == null || chapterView.getChapter() == null) {
        continue;
      }

      selectedIds.add(chapterView.getChapter().getId());
    }

    List<QuickSurahItem> items = new ArrayList<>();
    for (Integer chapterId : selectedIds) {
      ChapterView chapterView = chaptersById.get(chapterId);
      if (chapterView == null || chapterView.getChapter() == null) {
        continue;
      }

      Chapter chapter = chapterView.getChapter();
      String simpleName = safeText(chapter.getNameSimple(), "Surah " + chapterId);
      String arabicName = safeText(chapter.getNameArabic(), "");
      String verseCount = chapter.getVerseCount() > 0 ? chapter.getVerseCount() + " ayahs" : "Ayah count unavailable";
      String revelationPlace = safeText(chapter.getRevelationPlace(), "Unknown");

      items.add(new QuickSurahItem(
          chapterView,
          chapterId,
          simpleName,
          arabicName,
          verseCount + " " + '\u2022' + " " + revelationPlace));
    }

    return items;
  }

  private Map<Integer, ChapterView> indexChaptersById(List<ChapterView> chapters) {
    Map<Integer, ChapterView> map = new HashMap<>();
    for (ChapterView chapterView : chapters) {
      if (chapterView == null || chapterView.getChapter() == null) {
        continue;
      }

      map.put(chapterView.getChapter().getId(), chapterView);
    }
    return map;
  }

  private int resolveTranslationId(UserPreferenceService userPreferenceService) {
    if (userPreferenceService == null) {
      return FALLBACK_TRANSLATION_ID;
    }

    try {
      return userPreferenceService.getDefaultTranslation();
    } catch (Exception ignored) {
      return FALLBACK_TRANSLATION_ID;
    }
  }

  private String buildChapterLabel(ChapterView chapterView, int surahNumber) {
    if (chapterView == null || chapterView.getChapter() == null) {
      return "Surah " + surahNumber;
    }

    String simpleName = chapterView.getChapter().getNameSimple();
    if (simpleName == null || simpleName.isBlank()) {
      return "Surah " + surahNumber;
    }

    return surahNumber + ". " + simpleName;
  }

  private void renderPayload(HomePayload payload) {
    renderContinueReading(payload.continueReading());
    renderBookmarks(payload.bookmarkItems());
    renderQuickSurahs(payload.quickSurahs());
  }

  private void renderContinueReading(ContinueReadingItem item) {
    continueReadingContainer.getChildren().clear();

    if (item == null) {
      continueReadingSummaryLabel.setText("No reading progress yet.");
      continueReadingContainer.getChildren().add(createEmptyState("Start reading and your latest ayah will appear here."));
      return;
    }

    continueReadingSummaryLabel.setText("Resume from where you last paused.");
    continueReadingContainer.getChildren().add(createContinueReadingCard(item));
  }

  private void renderBookmarks(List<BookmarkItem> bookmarkItems) {
    recentBookmarksPane.getChildren().clear();

    if (bookmarkItems == null || bookmarkItems.isEmpty()) {
      bookmarksSummaryLabel.setText("No bookmarks yet.");
      recentBookmarksPane.getChildren().add(createEmptyState("Bookmark ayahs to keep them close for review."));
      return;
    }

    bookmarksSummaryLabel.setText(bookmarkItems.size() + " recent bookmarks");
    for (BookmarkItem item : bookmarkItems) {
      recentBookmarksPane.getChildren().add(createBookmarkCard(item));
    }
  }

  private void renderQuickSurahs(List<QuickSurahItem> quickSurahs) {
    quickSurahPane.getChildren().clear();

    if (quickSurahs == null || quickSurahs.isEmpty()) {
      quickSurahSummaryLabel.setText("No quick shortcuts available.");
      quickSurahPane.getChildren().add(createEmptyState("Shortcuts will appear when chapter data is available."));
      return;
    }

    quickSurahSummaryLabel.setText("Jump directly into familiar chapters.");
    for (QuickSurahItem item : quickSurahs) {
      quickSurahPane.getChildren().add(createQuickSurahCard(item));
    }
  }

  private Node createContinueReadingCard(ContinueReadingItem item) {
    VBox card = new VBox(8);
    card.getStyleClass().add("home-continue-card");

    Label titleLabel = new Label(item.chapterLabel() + " " + '\u2022' + " Ayah " + item.ayahNumber());
    titleLabel.getStyleClass().add("home-card-title");

    Label metaLabel = new Label(item.updatedLabel());
    metaLabel.getStyleClass().add("home-card-meta");

    Label arabicLabel = new Label(item.arabicText());
    arabicLabel.getStyleClass().add("home-card-arabic");
    arabicLabel.setWrapText(true);

    Label translationLabel = new Label(item.translationText());
    translationLabel.getStyleClass().add("home-card-translation");
    translationLabel.setWrapText(true);

    card.getChildren().addAll(titleLabel, metaLabel, arabicLabel, translationLabel);
    card.setOnMouseClicked(event -> openContinueReading(item));

    return card;
  }

  private Node createBookmarkCard(BookmarkItem item) {
    VBox card = new VBox(6);
    card.getStyleClass().add("home-bookmark-card");
    card.setPrefWidth(240);

    Label titleLabel = new Label(item.chapterLabel());
    titleLabel.getStyleClass().add("home-card-title");

    Label referenceLabel = new Label("Ayah " + item.ayahNumber());
    referenceLabel.getStyleClass().add("home-bookmark-reference");

    Label arabicLabel = new Label(item.arabicText());
    arabicLabel.getStyleClass().add("home-bookmark-arabic");
    arabicLabel.setWrapText(true);

    card.getChildren().addAll(titleLabel, referenceLabel, arabicLabel);
    card.setOnMouseClicked(event -> openBookmark(item));

    return card;
  }

  private Node createQuickSurahCard(QuickSurahItem item) {
    VBox card = new VBox(4);
    card.getStyleClass().add("home-surah-card");
    card.setPrefWidth(180);

    Label simpleNameLabel = new Label(item.surahNumber() + ". " + item.simpleName());
    simpleNameLabel.getStyleClass().add("home-surah-name");

    Label arabicNameLabel = new Label(item.arabicName());
    arabicNameLabel.getStyleClass().add("home-surah-arabic");
    arabicNameLabel.setWrapText(true);

    Label metaLabel = new Label(item.meta());
    metaLabel.getStyleClass().add("home-card-meta");

    card.getChildren().addAll(simpleNameLabel, arabicNameLabel, metaLabel);
    card.setOnMouseClicked(event -> openQuickSurah(item));

    return card;
  }

  private Label createEmptyState(String text) {
    Label label = new Label(text);
    label.getStyleClass().add("home-empty-state");
    label.setWrapText(true);
    return label;
  }

  private void openContinueReading(ContinueReadingItem item) {
    if (browsingController == null || item == null) {
      return;
    }

    ChapterView chapterView = item.chapterView();
    if (chapterView == null) {
      chapterView = LocalQuranQueryService.getInstance().getChapter(item.surahNumber(), CHAPTER_LANGUAGE).orElse(null);
    }

    if (chapterView == null) {
      ToastManager.getInstance().showWarning(
          "Continue Reading",
          "Unable to open Surah " + item.surahNumber() + ", Ayah " + item.ayahNumber() + ".");
      return;
    }

    int focusAyah = item.ayahNumber() > 0 ? item.ayahNumber() : 1;
    browsingController.showChapterAndFocusAyah(chapterView, focusAyah);
  }

  private void openBookmark(BookmarkItem item) {
    if (browsingController == null || item == null) {
      return;
    }

    ChapterView chapterView = item.chapterView();
    if (chapterView == null) {
      chapterView = LocalQuranQueryService.getInstance().getChapter(item.surahNumber(), CHAPTER_LANGUAGE).orElse(null);
    }

    if (chapterView == null) {
      ToastManager.getInstance().showWarning(
          "Bookmark",
          "Unable to open Surah " + item.surahNumber() + ", Ayah " + item.ayahNumber() + ".");
      return;
    }

    browsingController.showChapterAndFocusAyah(chapterView, item.ayahNumber());
  }

  private void openQuickSurah(QuickSurahItem item) {
    if (browsingController == null || item == null) {
      return;
    }

    ChapterView chapterView = item.chapterView();
    if (chapterView == null) {
      chapterView = LocalQuranQueryService.getInstance().getChapter(item.surahNumber(), CHAPTER_LANGUAGE).orElse(null);
    }

    if (chapterView == null) {
      ToastManager.getInstance().showWarning("Quick Surah", "Unable to open Surah " + item.surahNumber() + ".");
      return;
    }

    browsingController.showChapter(chapterView);
  }

  private void renderLoadFailure(String message) {
    continueReadingSummaryLabel.setText("Continue Reading is unavailable.");
    bookmarksSummaryLabel.setText("Bookmarks are unavailable.");
    quickSurahSummaryLabel.setText("Quick Surahs are unavailable.");

    continueReadingContainer.getChildren().setAll(createEmptyState(message));
    recentBookmarksPane.getChildren().setAll(createEmptyState(message));
    quickSurahPane.getChildren().setAll(createEmptyState(message));
  }

  private String safeText(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

  private record HomePayload(
      ContinueReadingItem continueReading,
      List<BookmarkItem> bookmarkItems,
      List<QuickSurahItem> quickSurahs) {
  }

  private record ContinueReadingItem(
      ChapterView chapterView,
      int surahNumber,
      int ayahNumber,
      String chapterLabel,
      String arabicText,
      String translationText,
      String updatedLabel) {
  }

  private record BookmarkItem(
      ChapterView chapterView,
      int surahNumber,
      int ayahNumber,
      String chapterLabel,
      String arabicText) {
  }

  private record QuickSurahItem(
      ChapterView chapterView,
      int surahNumber,
      String simpleName,
      String arabicName,
      String meta) {
  }
}
