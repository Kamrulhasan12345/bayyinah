package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.infra.local.query.*;
import com.ks.bayyinah.infra.local.database.*;
import com.ks.bayyinah.ui.ToastManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SearchResultsController {

  @FXML
  private TextField searchField;
  @FXML
  private Button searchButton;
  @FXML
  private Label resultCountLabel;
  @FXML
  private VBox resultsContainer;
  @FXML
  private HBox paginationBox;
  @FXML
  private Button firstPageBtn;
  @FXML
  private Button previousPageBtn;
  @FXML
  private Label pageInfoLabel;
  @FXML
  private Button nextPageBtn;
  @FXML
  private Button lastPageBtn;
  @FXML
  private ProgressIndicator loadingIndicator;
  @FXML
  private VBox noResultsBox;

  @Setter
  private LocalQuranSearchQueryService searchService;
  @Setter
  private LocalQuranQueryService quranQueryService;
  @Setter
  private BrowsingController browsingController;
  private final ToastManager toastManager;

  private SearchResponse currentSearchResponse;
  private String currentQuery;
  private Integer currentTranslationId = 20; // Default translation

  public SearchResultsController() {
    this.toastManager = ToastManager.getInstance();
  }

  /**
   * Initialize with a search query
   */
  public void initialize(String query) {
    this.currentQuery = query;
    searchField.setText(query);
    performSearch(query, 1);
  }

  @FXML
  private void initialize() {
    // Set up search on Enter key
    searchField.setOnAction(e -> onSearchClicked());

    // Set up pagination buttons
    firstPageBtn.setOnAction(e -> goToPage(1));
    previousPageBtn.setOnAction(e -> goToPreviousPage());
    nextPageBtn.setOnAction(e -> goToNextPage());
    lastPageBtn.setOnAction(e -> goToLastPage());

    // Initially hide pagination and no results
    paginationBox.setVisible(false);
    noResultsBox.setVisible(false);
  }

  /**
   * Perform search when button clicked or Enter pressed
   */
  @FXML
  private void onSearchClicked() {
    String query = searchField.getText().trim();

    if (query.isEmpty()) {
      toastManager.showWarning("Empty Query", "Please enter a search term");
      return;
    }

    currentQuery = query;
    performSearch(query, 1);
  }

  /**
   * Execute search and display results
   */
  private void performSearch(String query, int page) {
    // Show loading
    showLoading(true);

    // CompletableFuture.runAsync(() -> {
    // try {
    // PageRequest pageRequest = PageRequest.of(page, 20);
    // SearchResponse response = searchService.search(query, currentTranslationId,
    // pageRequest);
    //
    // Platform.runLater(() -> {
    // currentSearchResponse = response;
    // displayResults(response);
    // showLoading(false);
    // });
    //
    // } catch (Exception e) {
    // Platform.runLater(() -> {
    // toastManager.showError("Search Failed", "Failed to search: " +
    // e.getMessage());
    // showLoading(false);
    // });
    // }
    // });

    DbAsync.runWithUi(() -> {
      PageRequest pageRequest = PageRequest.of(page, 20);
      SearchResponse response = searchService.search(query, currentTranslationId, pageRequest);
      return response;

    }, (response) -> {
      currentSearchResponse = response;
      displayResults(response);
      showLoading(false);

    }, (e) -> {
      toastManager.showError("Search Failed", "Failed to search: " + e.getMessage());
      showLoading(false);
    });
  }

  /**
   * Display search results
   */
  private void displayResults(SearchResponse response) {
    resultsContainer.getChildren().clear();

    // Check if no results
    if (response.getTotalResults() <= 0) {
      showNoResults();
      return;
    }

    noResultsBox.setVisible(false);

    // Update result count
    updateResultCount(response);

    // Display based on search type
    switch (response.getSearchType()) {
      case VERSE_KEY:
      case VERSES:
        displayVerseResults(response.getVerseResults());
        break;

      case CHAPTERS:
        displayChapterResults(response.getChapterResults());
        break;

      default:
        break;
    }

    // Update pagination
    updatePagination(response);
  }

  /**
   * Display verse search results
   */
  private void displayVerseResults(Page<VerseSearchResult> page) {
    for (VerseSearchResult result : page.getContent()) {
      VBox resultCard = createVerseResultCard(result);
      resultsContainer.getChildren().add(resultCard);
    }
  }

  /**
   * Display chapter search results
   */
  private void displayChapterResults(Page<ChapterSearchResult> page) {
    for (ChapterSearchResult result : page.getContent()) {
      VBox resultCard = createChapterResultCard(result);
      resultsContainer.getChildren().add(resultCard);
    }
  }

  /**
   * Create verse result card
   */
  private VBox createVerseResultCard(VerseSearchResult result) {
    VBox card = new VBox(8);
    card.getStyleClass().add("search-result-card");
    card.setPadding(new Insets(15));
    card.setOnMouseClicked(e -> navigateToVerse(result));

    // Verse reference (e.g., "Al-Fatiha 1:1")
    Label referenceLabel = new Label(result.getVerseView().getVerseKey());
    referenceLabel.getStyleClass().add("verse-reference");

    // Arabic text
    Text arabicText = new Text(result.getVerseView().getArabicText());
    arabicText.getStyleClass().add("arabic-text");
    arabicText.setWrappingWidth(750);

    TextFlow arabicFlow = new TextFlow(arabicText);
    arabicFlow.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);

    // Translation text (if available)
    VBox translationBox = null;
    if (result.getVerseView().getTranslatedText() != null) {
      Label translationLabel = new Label(result.getVerseView().getTranslatedText());
      translationLabel.getStyleClass().add("translation-text");
      translationLabel.setWrapText(true);
      translationLabel.setMaxWidth(750);

      translationBox = new VBox(translationLabel);
      translationBox.setPadding(new Insets(5, 0, 0, 0));
    }

    // Highlighted text (if different from original)
    TextFlow highlightedFlow = null;
    if (result.getHighlightedText() != null && !result.getHighlightedText().equals(result.getDisplayText())) {
      highlightedFlow = createHighlightedText(result.getHighlightedText());
    }

    // Match indicator
    Label matchLabel = new Label(getMatchIndicatorText(result.getMatchedIn()));
    matchLabel.getStyleClass().add("match-indicator");

    // Add all components
    card.getChildren().addAll(referenceLabel, arabicFlow);

    if (highlightedFlow != null) {
      card.getChildren().add(highlightedFlow);
    }

    if (translationBox != null) {
      card.getChildren().add(translationBox);
    }

    card.getChildren().add(matchLabel);

    // Hover effect
    card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f5f5f5; -fx-cursor: hand;"));
    card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white;"));

    return card;
  }

  /**
   * Create chapter result card
   */
  private VBox createChapterResultCard(ChapterSearchResult result) {
    VBox card = new VBox(8);
    card.getStyleClass().add("search-result-card");
    card.setPadding(new Insets(15));
    card.setOnMouseClicked(e -> navigateToChapter(result));

    // Chapter name (English)
    Label nameLabel = new Label(result.getChapterView().getChapter().getNameSimple());
    nameLabel.getStyleClass().add("chapter-name");

    // Chapter name (Arabic)
    Label arabicLabel = new Label(result.getChapterView().getChapter().getNameArabic());
    arabicLabel.getStyleClass().add("chapter-name-arabic");

    // Chapter info
    String info = String.format("%d verses • Revealed in %s",
        result.getChapterView().getChapter().getVerseCount(),
        result.getChapterView().getChapter().getRevelationPlace());
    Label infoLabel = new Label(info);
    infoLabel.getStyleClass().add("chapter-info");

    // Translated name (if available)
    if (result.getChapterView().getChapterI18N() != null) {
      Label translatedLabel = new Label(result.getChapterView().getChapterI18N().getTranslatedName());
      translatedLabel.getStyleClass().add("chapter-translated-name");
      card.getChildren().add(translatedLabel);
    }

    card.getChildren().addAll(nameLabel, arabicLabel, infoLabel);

    // Hover effect
    card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f5f5f5; -fx-cursor: hand;"));
    card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white;"));

    return card;
  }

  /**
   * Create highlighted text with bold markers
   */
  private TextFlow createHighlightedText(String highlightedText) {
    TextFlow textFlow = new TextFlow();
    textFlow.setPadding(new Insets(5, 0, 0, 0));

    // Split by highlight markers (**)
    String[] parts = highlightedText.split("\\*\\*");

    for (int i = 0; i < parts.length; i++) {
      Text text = new Text(parts[i]);

      // Odd indices are highlighted (between **)
      if (i % 2 == 1) {
        text.setStyle("-fx-font-weight: bold; -fx-fill: #FF9800;");
      } else {
        text.getStyleClass().add("highlighted-context");
      }

      textFlow.getChildren().add(text);
    }

    return textFlow;
  }

  /**
   * Get match indicator text
   */
  private String getMatchIndicatorText(VerseSearchResult.MatchLocation matchedIn) {
    switch (matchedIn) {
      case ARABIC_TEXT:
        return "📖 Matched in Arabic text";
      case TRANSLATION:
        return "🌍 Matched in translation";
      case VERSE_KEY:
        return "🔑 Direct verse reference";
      default:
        return "";
    }
  }

  /**
   * Navigate to verse
   */
  private void navigateToVerse(VerseSearchResult result) {
    int chapterId = result.getVerseView().getVerse().getSurahId();
    int verseNumber = result.getVerseView().getVerse().getVerseNumber();

    Optional<ChapterView> chapterView = quranQueryService.getChapter(chapterId, "en");
    browsingController.showChapter(chapterView.get(), verseNumber, verseNumber, currentTranslationId);

    toastManager.showInfo("Navigating", "Opening " + result.getVerseView().getVerseKey());
  }

  /**
   * Navigate to chapter
   */
  private void navigateToChapter(ChapterSearchResult result) {
    int chapterId = result.getChapterView().getChapter().getId();

    // Navigate to entire chapter (verse 1 to last)
    int lastVerse = result.getChapterView().getChapter().getVerseCount();
    browsingController.showChapter(result.getChapterView(), 1, lastVerse, currentTranslationId);

    toastManager.showInfo("Navigating", "Opening " + result.getChapterView().getChapter().getNameSimple());
  }

  /**
   * Update result count label
   */
  private void updateResultCount(SearchResponse response) {
    long total = response.getTotalResults();

    String countText;
    if (total == 0) {
      countText = "No results found";
    } else if (total == 1) {
      countText = "1 result found";
    } else {
      countText = String.format("%,d results found", total);
    }

    resultCountLabel.setText(countText + " for \"" + currentQuery + "\"");
  }

  /**
   * Update pagination controls
   */
  private void updatePagination(SearchResponse response) {
    Page<?> page = response.getVerseResults() != null
        ? response.getVerseResults()
        : response.getChapterResults();

    if (page == null || page.getTotalPages() <= 1) {
      paginationBox.setVisible(false);
      return;
    }

    paginationBox.setVisible(true);

    // Update page info
    pageInfoLabel.setText(String.format("Page %d of %d", page.getPage(), page.getTotalPages()));

    // Enable/disable buttons
    firstPageBtn.setDisable(page.isFirst());
    previousPageBtn.setDisable(!page.hasPrevious());
    nextPageBtn.setDisable(!page.hasNext());
    lastPageBtn.setDisable(page.isLast());
  }

  /**
   * Show no results message
   */
  private void showNoResults() {
    noResultsBox.setVisible(true);
    resultCountLabel.setText("No results found for \"" + currentQuery + "\"");
    paginationBox.setVisible(false);
  }

  /**
   * Show/hide loading indicator
   */
  private void showLoading(boolean loading) {
    loadingIndicator.setVisible(loading);
    resultsContainer.setVisible(!loading);
    paginationBox.setVisible(!loading);

    if (loading) {
      resultCountLabel.setText("Searching...");
    }
  }

  // ═══════════════════════════════════════════════════════════
  // PAGINATION METHODS
  // ═══════════════════════════════════════════════════════════

  private void goToPage(int page) {
    performSearch(currentQuery, page);
  }

  private void goToPreviousPage() {
    if (currentSearchResponse != null) {
      Page<?> page = currentSearchResponse.getVerseResults() != null
          ? currentSearchResponse.getVerseResults()
          : currentSearchResponse.getChapterResults();

      if (page != null && page.hasPrevious()) {
        goToPage(page.getPage() - 1);
      }
    }
  }

  private void goToNextPage() {
    if (currentSearchResponse != null) {
      Page<?> page = currentSearchResponse.getVerseResults() != null
          ? currentSearchResponse.getVerseResults()
          : currentSearchResponse.getChapterResults();

      if (page != null && page.hasNext()) {
        goToPage(page.getPage() + 1);
      }
    }
  }

  private void goToLastPage() {
    if (currentSearchResponse != null) {
      Page<?> page = currentSearchResponse.getVerseResults() != null
          ? currentSearchResponse.getVerseResults()
          : currentSearchResponse.getChapterResults();

      if (page != null) {
        goToPage(page.getTotalPages());
      }
    }
  }

  /**
   * Set translation ID for searches
   */
  public void setTranslationId(Integer translationId) {
    this.currentTranslationId = translationId;
  }
}
