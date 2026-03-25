package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.TranslationView;
import com.ks.bayyinah.core.model.Translation;
import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.hybrid.service.UserPreferenceService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.ui.ToastManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import lombok.Setter;

public class SettingsController {

  private static final String KEY_THEME = "theme";
  private static final String KEY_FONT_SIZE = "font_size";
  private static final String KEY_DEFAULT_TRANSLATION = "default_translation";
  private static final String KEY_LANGUAGE = "language";
  private static final String KEY_READING_MODE = "reading_mode";
  private static final String KEY_SHOW_TRANSLITERATION = "show_transliteration";
  private static final String KEY_AUTO_SCROLL = "auto_scroll";

  @FXML
  private ComboBox<String> themeCombo;

  @FXML
  private Spinner<Integer> fontSizeSpinner;

  @FXML
  private ComboBox<TranslationOption> defaultTranslationCombo;

  @FXML
  private Button downloadTranslationButton;

  @FXML
  private Label translationAvailabilityLabel;

  @FXML
  private HBox translationAvailabilityBox;

  @FXML
  private ComboBox<String> languageCombo;

  @FXML
  private ComboBox<String> readingModeCombo;

  @FXML
  private CheckBox showTransliterationCheck;

  @FXML
  private CheckBox autoScrollCheck;

  @FXML
  private TextField idField;

  @FXML
  private TextField userIdField;

  @FXML
  private TextField createdAtField;

  @FXML
  private TextField updatedAtField;

  @FXML
  private Label modeLabel;

  @FXML
  private Label lastSavedLabel;

  @FXML
  private Button saveButton;

  @FXML
  private Button syncNowButton;

  @Setter
  private AppContext appContext;

  private int selectedDefaultTranslationId = 20;

  public void initializeSettings() {
    configureInputs();
    loadPreferences();
  }

  @FXML
  private void onSaveChanges() {
    if (appContext == null || appContext.getUserPreferenceService() == null) {
      ToastManager.getInstance().showError("Settings", "Preference service is not available.");
      return;
    }

    final String theme = coalesce(themeCombo.getValue(), "light");
    final int fontSize = fontSizeSpinner.getValue();
    final int defaultTranslation = selectedDefaultTranslationId;
    final String language = coalesce(languageCombo.getValue(), "en");
    final String readingMode = coalesce(readingModeCombo.getValue(), "continuous");
    final boolean showTransliteration = showTransliterationCheck.isSelected();
    final boolean autoScroll = autoScrollCheck.isSelected();

    if (fontSize < 10 || fontSize > 48) {
      ToastManager.getInstance().showWarning("Settings", "Font size must be between 10 and 48.");
      return;
    }

    if (defaultTranslation <= 0) {
      ToastManager.getInstance().showWarning("Settings", "Default translation ID must be greater than 0.");
      return;
    }

    saveButton.setDisable(true);
    UserPreferenceService preferenceService = appContext.getUserPreferenceService();

    DbAsync.runWithUi(() -> {
      Map<String, String> updates = new LinkedHashMap<>();
      updates.put(KEY_THEME, theme);
      updates.put(KEY_FONT_SIZE, String.valueOf(fontSize));
      updates.put(KEY_DEFAULT_TRANSLATION, String.valueOf(defaultTranslation));
      updates.put(KEY_LANGUAGE, language);
      updates.put(KEY_READING_MODE, readingMode);
      updates.put(KEY_SHOW_TRANSLITERATION, String.valueOf(showTransliteration));
      updates.put(KEY_AUTO_SCROLL, String.valueOf(autoScroll));
      preferenceService.setPreferences(updates);
      return updates.size();
    }, updatedCount -> {
      saveButton.setDisable(false);
      lastSavedLabel.setText("Saved locally. " + updatedCount + " settings queued for sync.");
      ToastManager.getInstance().showSuccess("Settings", "Preferences saved locally.");
    }, err -> {
      saveButton.setDisable(false);
      err.printStackTrace();
      ToastManager.getInstance().showError("Settings", "Failed to save settings: " + err.getMessage());
    });
  }

  @FXML
  private void onSyncNow() {
    if (appContext == null || appContext.getSyncOrchestratorService() == null) {
      ToastManager.getInstance().showError("Sync", "Sync service is not available.");
      return;
    }

    if (syncNowButton != null) {
      syncNowButton.setDisable(true);
    }
    lastSavedLabel.setText("Sync in progress...");

    DbAsync.runWithUi(() -> {
      appContext.getSyncOrchestratorService().runSyncNow();
      return true;
    }, ignored -> {
      if (syncNowButton != null) {
        syncNowButton.setDisable(false);
      }
      lastSavedLabel.setText("Sync completed successfully.");
      ToastManager.getInstance().showSuccess("Sync", "Cloud sync completed.");
    }, err -> {
      if (syncNowButton != null) {
        syncNowButton.setDisable(false);
      }
      String message = err.getMessage() == null ? "Unknown error" : err.getMessage();
      lastSavedLabel.setText("Sync failed: " + message);
      ToastManager.getInstance().showWarning("Sync", "Cloud sync failed. Data remains local.");
    });
  }

  private void configureInputs() {
    modeLabel.setText("Mode: Local-first (changes are saved in SQLite and queued for sync)");
    lastSavedLabel.setText("No pending save in this session.");

    themeCombo.setItems(FXCollections.observableArrayList("light", "dark", "sepia"));
    themeCombo.setEditable(true);

    languageCombo.setItems(FXCollections.observableArrayList("en", "ar", "ur", "bn", "tr", "id"));
    languageCombo.setEditable(true);

    readingModeCombo.setItems(FXCollections.observableArrayList("continuous", "paged"));
    readingModeCombo.setEditable(true);

    fontSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 48, 16));

    fontSizeSpinner.setEditable(true);

    translationAvailabilityBox.setVisible(false);
    translationAvailabilityBox.setManaged(false);

    defaultTranslationCombo.setOnAction(event -> {
      TranslationOption selected = defaultTranslationCombo.getValue();
      if (selected == null) {
        return;
      }
      selectedDefaultTranslationId = selected.id();
      updateTranslationAvailabilityUi(selected);
    });

    idField.setText("Server generated");
    userIdField.setText("Server managed");
    createdAtField.setText("Server managed");
    updatedAtField.setText("Server managed");
  }

  private void loadPreferences() {
    if (appContext == null || appContext.getUserPreferenceService() == null) {
      applyDefaults();
      return;
    }

    UserPreferenceService preferenceService = appContext.getUserPreferenceService();

    DbAsync.runWithUi(() -> {
      Map<String, String> values = new LinkedHashMap<>();
      values.put(KEY_THEME, getPreferenceOrDefault(preferenceService, KEY_THEME, "light"));
      values.put(KEY_FONT_SIZE, getPreferenceOrDefault(preferenceService, KEY_FONT_SIZE, "16"));
      values.put(KEY_DEFAULT_TRANSLATION, getPreferenceOrDefault(preferenceService, KEY_DEFAULT_TRANSLATION, "20"));
      values.put(KEY_LANGUAGE, getPreferenceOrDefault(preferenceService, KEY_LANGUAGE, "en"));
      values.put(KEY_READING_MODE, getPreferenceOrDefault(preferenceService, KEY_READING_MODE, "continuous"));
      values.put(KEY_SHOW_TRANSLITERATION, getPreferenceOrDefault(preferenceService, KEY_SHOW_TRANSLITERATION, "false"));
      values.put(KEY_AUTO_SCROLL, getPreferenceOrDefault(preferenceService, KEY_AUTO_SCROLL, "true"));
      values.put("id", getPreferenceOrDefault(preferenceService, "id", "Server generated"));
      values.put("user_id", getPreferenceOrDefault(preferenceService, "user_id", "Server managed"));
      values.put("created_at", getPreferenceOrDefault(preferenceService, "created_at", "Server managed"));
      values.put("updated_at", getPreferenceOrDefault(preferenceService, "updated_at", "Server managed"));
      return values;
    }, values -> {
      applyPreferenceValues(values);
      loadTranslationOptions(selectedDefaultTranslationId);
    }, err -> {
      err.printStackTrace();
      applyDefaults();
      loadTranslationOptions(selectedDefaultTranslationId);
      ToastManager.getInstance().showWarning("Settings", "Could not load saved preferences. Defaults loaded.");
    });
  }

  private void applyPreferenceValues(Map<String, String> values) {
    themeCombo.setValue(values.get(KEY_THEME));
    fontSizeSpinner.getValueFactory().setValue(parseInt(values.get(KEY_FONT_SIZE), 16));
    selectedDefaultTranslationId = parseInt(values.get(KEY_DEFAULT_TRANSLATION), 20);
    languageCombo.setValue(values.get(KEY_LANGUAGE));
    readingModeCombo.setValue(values.get(KEY_READING_MODE));
    showTransliterationCheck.setSelected(Boolean.parseBoolean(values.get(KEY_SHOW_TRANSLITERATION)));
    autoScrollCheck.setSelected(Boolean.parseBoolean(values.get(KEY_AUTO_SCROLL)));

    idField.setText(values.get("id"));
    userIdField.setText(values.get("user_id"));
    createdAtField.setText(values.get("created_at"));
    updatedAtField.setText(values.get("updated_at"));
  }

  private void applyDefaults() {
    themeCombo.setValue("light");
    fontSizeSpinner.getValueFactory().setValue(16);
    selectedDefaultTranslationId = 20;
    languageCombo.setValue("en");
    readingModeCombo.setValue("continuous");
    showTransliterationCheck.setSelected(false);
    autoScrollCheck.setSelected(true);
  }

  @FXML
  private void onDownloadTranslation() {
    TranslationOption selected = defaultTranslationCombo.getValue();
    if (selected == null) {
      ToastManager.getInstance().showInfo("Translation", "Select a translation first.");
      return;
    }

    if (selected.available()) {
      ToastManager.getInstance().showSuccess("Translation", "This translation is already available locally.");
      return;
    }

    ToastManager.getInstance().showInfo(
        "Download Translation",
        "Download is not wired yet. This will be connected in the next step.");
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
                buildTranslationLabel(translation, availableIds.contains(translation.getId())),
                availableIds.contains(translation.getId())));
          }

          if (options.isEmpty()) {
            options.add(new TranslationOption(activeTranslationId, "Translation " + activeTranslationId + " (unavailable)", false));
          }

          defaultTranslationCombo.setItems(FXCollections.observableArrayList(options));

          TranslationOption selected = null;
          for (TranslationOption option : options) {
            if (option.id() == activeTranslationId) {
              selected = option;
              break;
            }
          }

          if (selected == null) {
            selected = options.get(0);
          }

          defaultTranslationCombo.getSelectionModel().select(selected);
          selectedDefaultTranslationId = selected.id();
          updateTranslationAvailabilityUi(selected);
        },
        err -> {
          err.printStackTrace();
          TranslationOption fallback = new TranslationOption(activeTranslationId,
              "Translation " + activeTranslationId + " (unavailable)", false);
          defaultTranslationCombo.setItems(FXCollections.observableArrayList(fallback));
          defaultTranslationCombo.getSelectionModel().select(fallback);
          selectedDefaultTranslationId = fallback.id();
          updateTranslationAvailabilityUi(fallback);
        });
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

  private void updateTranslationAvailabilityUi(TranslationOption option) {
    if (option == null) {
      translationAvailabilityBox.setVisible(false);
      translationAvailabilityBox.setManaged(false);
      downloadTranslationButton.setDisable(true);
      return;
    }

    if (option.available()) {
      translationAvailabilityBox.setVisible(false);
      translationAvailabilityBox.setManaged(false);
      downloadTranslationButton.setDisable(true);
      return;
    }

    translationAvailabilityLabel.setText("This translation is unavailable locally. Download is required before it can be shown in verses.");
    translationAvailabilityBox.setVisible(true);
    translationAvailabilityBox.setManaged(true);
    downloadTranslationButton.setDisable(false);
  }

  private String getPreferenceOrDefault(UserPreferenceService service, String key, String defaultValue) {
    var preference = service.getPreference(key);
    if (preference == null || preference.getValue() == null || preference.getValue().isBlank()) {
      return defaultValue;
    }
    return preference.getValue();
  }

  private String coalesce(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value.trim();
  }

  private int parseInt(String value, int fallback) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return fallback;
    }
  }

  private record TranslationOption(int id, String label, boolean available) {
    @Override
    public String toString() {
      return label;
    }
  }

  private record TranslationOptionsPayload(List<TranslationView> translations, Set<Integer> availableTranslationIds) {
  }
}
