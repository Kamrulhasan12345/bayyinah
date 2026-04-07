package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.remote.dto.recsys.RecommendResponse;
import com.ks.bayyinah.infra.remote.dto.recsys.VerseResponse;
import com.ks.bayyinah.infra.remote.query.RemoteAIQueryService;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AIChatController {

  private static final int DEFAULT_TOP_K = 5;
  private static final String DEFAULT_LANGUAGE = "english";

  @FXML
  private TextField promptField;

  @FXML
  private Button sendButton;

  @FXML
  private Label statusLabel;

  @FXML
  private Label promptEchoLabel;

  // @FXML
  // private Label emotionsLabel;

  @FXML
  private Hyperlink verseLink;

  @FXML
  private Label reflectionProviderLabel;

  @FXML
  private Label arabicLabel;

  @FXML
  private Label translationLabel;

  @FXML
  private Label reflectionLabel;

  @FXML
  private Label metricsLabel;

  @FXML
  private Button previousButton;

  @FXML
  private Button nextButton;

  @FXML
  private Label indexLabel;

  private AppContext appContext;
  private BrowsingController browsingController;
  private RemoteAIQueryService remoteAIQueryService;

  private RecommendResponse currentResponse;
  private int currentVerseIndex = -1;
  private String lastPrompt = "";

  void setAppContext(AppContext appContext) {
    this.appContext = appContext;
    this.remoteAIQueryService = appContext != null ? appContext.getRemoteAIQueryService() : null;
  }

  void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  void initializeAiChat() {
    if (currentResponse != null) {
      renderCurrentVerse();
      return;
    }

    setEmptyState("Ask a question and Bayyinah AI will return reflective verses.");
  }

  @FXML
  private void onSendPrompt() {
    String prompt = promptField.getText() != null ? promptField.getText().trim() : "";
    if (prompt.isBlank()) {
      statusLabel.setText("Please enter a message first.");
      return;
    }

    if (remoteAIQueryService == null) {
      statusLabel.setText("AI service is unavailable right now.");
      return;
    }

    lastPrompt = prompt;
    setLoading(true);
    statusLabel.setText("Thinking...");

    remoteAIQueryService.getRecommendationsWithReflections(prompt, DEFAULT_TOP_K, DEFAULT_LANGUAGE)
        .thenAccept(response -> Platform.runLater(() -> applyResponse(prompt, response)))
        .exceptionally(error -> {
          Platform.runLater(() -> {
            setLoading(false);
            statusLabel.setText("Request failed: " + simplifyError(error));
          });
          return null;
        });
  }

  @FXML
  private void onPreviousResult() {
    if (currentResponse == null || currentResponse.verses() == null || currentVerseIndex <= 0) {
      return;
    }

    currentVerseIndex--;
    renderCurrentVerse();
  }

  @FXML
  private void onNextResult() {
    if (currentResponse == null || currentResponse.verses() == null
        || currentVerseIndex >= currentResponse.verses().size() - 1) {
      return;
    }

    currentVerseIndex++;
    renderCurrentVerse();
  }

  @FXML
  private void onOpenVerseInReader() {
    VerseResponse verse = currentVerse();
    if (verse == null) {
      return;
    }

    if (browsingController == null) {
      statusLabel.setText("Reader navigation is currently unavailable.");
      return;
    }

    statusLabel.setText("Opening Surah " + verse.surah() + ", Ayah " + verse.ayah() + "...");
    browsingController.navigateToVerseFromAi(verse.surah(), verse.ayah());
  }

  private void applyResponse(String prompt, RecommendResponse response) {
    setLoading(false);
    currentResponse = response;

    promptEchoLabel.setText("Prompt: " + firstNonBlank(response != null ? response.query() : null, prompt));
    // emotionsLabel.setText("Detected emotions: "
    //     + formatList(response != null ? response.detectedEmotions() : null));

    if (response == null || response.verses() == null || response.verses().isEmpty()) {
      currentVerseIndex = -1;
      setEmptyState("No recommendations were returned for this prompt.");
      return;
    }

    currentVerseIndex = 0;
    renderCurrentVerse();
    statusLabel.setText("Found " + response.verses().size() + " recommendations.");
  }

  private void renderCurrentVerse() {
    VerseResponse verse = currentVerse();
    if (verse == null) {
      setEmptyState("No verse is selected.");
      return;
    }

    verseLink.setDisable(false);
    verseLink.setText("Surah " + verse.surah() + " : Ayah " + verse.ayah() + " (Open in Reader)");

    arabicLabel.setText(firstNonBlank(verse.arabic(), verse.text(), "Arabic text unavailable."));
    translationLabel.setText(formatTranslation(verse));

    String reflection = firstNonBlank(verse.reflection(), "No reflection provided for this verse.");
    reflectionLabel.setText(formatReflectionText(reflection));

    reflectionProviderLabel.setText("Provider: "
        + firstNonBlank(verse.reflectionProvider(), "not specified"));

    metricsLabel.setText(formatMetrics(verse));
    updateNavigationControls();
  }

  private VerseResponse currentVerse() {
    if (currentResponse == null || currentResponse.verses() == null || currentResponse.verses().isEmpty()) {
      return null;
    }

    if (currentVerseIndex < 0 || currentVerseIndex >= currentResponse.verses().size()) {
      return null;
    }

    return currentResponse.verses().get(currentVerseIndex);
  }

  private void setLoading(boolean loading) {
    sendButton.setDisable(loading);
    promptField.setDisable(loading);
    if (loading) {
      previousButton.setDisable(true);
      nextButton.setDisable(true);
      return;
    }

    updateNavigationControls();
  }

  private void setEmptyState(String status) {
    statusLabel.setText(status);
    verseLink.setText("No verse selected");
    verseLink.setDisable(true);
    reflectionProviderLabel.setText("Provider: not specified");
    arabicLabel.setText("-");
    translationLabel.setText("-");
    reflectionLabel.setText("-");
    metricsLabel.setText("-");
    updateNavigationControls();
  }

  private void updateNavigationControls() {
    int total = currentResponse != null && currentResponse.verses() != null ? currentResponse.verses().size() : 0;

    if (total <= 0 || currentVerseIndex < 0) {
      previousButton.setDisable(true);
      nextButton.setDisable(true);
      indexLabel.setText("0 / 0");
      return;
    }

    previousButton.setDisable(currentVerseIndex <= 0);
    nextButton.setDisable(currentVerseIndex >= total - 1);
    indexLabel.setText((currentVerseIndex + 1) + " / " + total);
  }

  private String formatList(List<String> values) {
    if (values == null || values.isEmpty()) {
      return "none";
    }

    return values.stream()
        .filter(item -> item != null && !item.isBlank())
        .collect(Collectors.joining(", "));
  }

  private String formatMetrics(VerseResponse verse) {
    return String.format(Locale.US,
        "Relevance %s | Semantic %s | Distance %s",
        formatMetricValue(verse.relevanceScore()),
        formatMetricValue(verse.semanticScore()),
        formatMetricValue(verse.semanticDistance()));
  }

  private String formatMetricValue(Float value) {
    return value == null ? "n/a" : String.format(Locale.US, "%.2f", value);
  }
  private String formatTranslation(VerseResponse verse) {
    String english = normalize(verse.translationEn());
    String urdu = normalize(verse.translationUr());

    if (!english.isBlank() && !urdu.isBlank()) {
      return "English:\n" + english + "\n\nUrdu:\n" + urdu;
    }

    String fallback = firstNonBlank(english, urdu, normalize(verse.text()));
    if (fallback.isBlank()) {
      return "Translation unavailable.";
    }

    return fallback;
  }

  private String formatReflectionText(String reflection) {
    String value = normalize(reflection);
    if (value.isBlank()) {
      return "No reflection provided for this verse.";
    }

    return "\"" + value + "\"";
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }

    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }

    return "";
  }

  private String simplifyError(Throwable error) {
    Throwable cursor = error;
    while (cursor.getCause() != null) {
      cursor = cursor.getCause();
    }

    String message = cursor.getMessage();
    if (message == null || message.isBlank()) {
      return "Unknown error";
    }

    return message;
  }
}
