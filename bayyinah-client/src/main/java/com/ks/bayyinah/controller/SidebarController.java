package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.controller.cell.ChapterSidebarCell;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.ui.ToastManager;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import lombok.Setter;
import org.kordamp.ikonli.javafx.FontIcon;

public class SidebarController {

  @FXML
  private ListView<ChapterView> chaptersListView;

  @FXML
  private TextField searchField;

  @FXML
  private FontIcon homeBtn;

  @FXML
  private FontIcon settingsBtn;

  @FXML
  private FontIcon toggleAuthBtn;

  @FXML
  private Label username;

  @Setter
  private Consumer<ChapterView> onChapterSelected;

  @Setter
  private Runnable onLoginClicked;

  @Setter
  private Runnable onHomeBtnClick;

  @Setter
  private Runnable onSettingsClicked;

  private List<ChapterView> allChapters;
  private PauseTransition searchDebounce;
  private final ObservableList<ChapterView> displayedChapters = FXCollections.observableArrayList();
  private int lastEmittedChapterId = -1;

  @Setter
  private AppContext appContext;

  public void initializeSidebar() {

    /*
     * Sidebar Chapters List Fetching and Display Logic
     */
    LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();

    searchDebounce = new PauseTransition(Duration.millis(300));
    searchDebounce.setOnFinished(event -> filterChapters(searchField.getText()));

    setupHomeButton();
    setupSettingsButton();
    setupKeyboardNavigation();

    chaptersListView.setItems(displayedChapters);
    chaptersListView.setFixedCellSize(60);

    DbAsync.runWithUi(() -> quranQueryService.getAllChapters("en"),
        chapters -> {
          allChapters = chapters;
          displayedChapters.setAll(chapters);
        });

    chaptersListView.setCellFactory(listView -> new ChapterSidebarCell());

    chaptersListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, old, selected) -> {
          emitChapterSelection(selected, false);
        });

    searchField
        .textProperty()
        .addListener((obs, oldText, newText) -> {
          searchDebounce.playFromStart(); // Reset timer on each keystroke
        });

    /*
     * User Info Display Logic (e.g., username / Guest User)
     */

    refreshAuthState();

    /*
     * Settings Button Logic
     */
  }

  private void filterChapters(String keyword) {
    if (allChapters == null) {
      return;
    }

    ChapterView selectedChapter = chaptersListView.getSelectionModel().getSelectedItem();
    Integer selectedChapterId = selectedChapter != null ? selectedChapter.getChapter().getId() : null;

    List<ChapterView> filtered;
    if (keyword == null || keyword.isBlank()) {
      filtered = allChapters;
    } else {
      String lowerKeyword = keyword.toLowerCase();
      filtered = allChapters
          .stream()
          .filter(cv -> {
            String nameSimple = cv.getChapter().getNameSimple();
            String nameArabic = cv.getChapter().getNameArabic();
            String nameTranslated = cv.getChapterI18N() != null
                ? cv.getChapterI18N().getTranslatedName()
                : null;
            return ((nameSimple != null &&
                nameSimple.toLowerCase().contains(lowerKeyword)) ||
                (nameArabic != null && nameArabic.contains(keyword)) || (nameTranslated != null &&
                    nameTranslated.toLowerCase().contains(lowerKeyword)));
          })
          .collect(Collectors.toList());
    }

    displayedChapters.setAll(filtered);

    if (selectedChapterId != null) {
      for (ChapterView chapterView : displayedChapters) {
        if (chapterView.getChapter().getId() == selectedChapterId) {
          chaptersListView.getSelectionModel().select(chapterView);
          break;
        }
      }
    }
  }

  private void setupHomeButton() {
    if (homeBtn != null) {
      homeBtn.setOnMouseClicked(e -> {
        clearSelection();
        if (onHomeBtnClick != null) {
          onHomeBtnClick.run();
        }
      });
    }
  }

  private void setupSettingsButton() {
    if (settingsBtn != null) {
      settingsBtn.setOnMouseClicked(e -> {
        if (onSettingsClicked != null) {
          onSettingsClicked.run();
        }
      });
    }
  }

  private void setupKeyboardNavigation() {
    chaptersListView.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        ChapterView selected = chaptersListView.getSelectionModel().getSelectedItem();
        emitChapterSelection(selected, true);
      }
    });
  }

  private void emitChapterSelection(ChapterView selected, boolean forceEmit) {
    if (selected == null || onChapterSelected == null) {
      return;
    }

    int selectedChapterId = selected.getChapter().getId();
    if (!forceEmit && selectedChapterId == lastEmittedChapterId) {
      return;
    }

    lastEmittedChapterId = selectedChapterId;
    onChapterSelected.accept(selected);
  }

  public void clearSelection() {
    Platform.runLater(() -> {
      chaptersListView.getSelectionModel().clearSelection();
      lastEmittedChapterId = -1;
    });
  }

  public void refreshAuthState() {
    AuthSessionQueryService authSessionQueryService = appContext.getAuthSessionQueryService();

    DbAsync.runWithUi(authSessionQueryService::getCurrentUser, user -> {
      username.setText(user.getDisplayName());

      if (user.isGuest()) {
        bindGuestAuthAction();
      } else {
        bindLogoutAction(authSessionQueryService);
      }
    }, e -> {
      e.printStackTrace();
      username.setText("Guest User");
      bindGuestAuthAction();
      ToastManager.getInstance().showWarning("Authentication", "Unable to refresh auth state. Using guest mode.");
    });
  }

  private void bindGuestAuthAction() {
    username.setText("Guest User");
    toggleAuthBtn.setIconLiteral("mdi2l-login");
    toggleAuthBtn.setOnMouseClicked(e -> {
      if (onLoginClicked != null) {
        onLoginClicked.run();
      }
    });
  }

  private void bindLogoutAction(AuthSessionQueryService authSessionQueryService) {
    toggleAuthBtn.setIconLiteral("mdi2l-logout");
    toggleAuthBtn.setOnMouseClicked(e -> {
      DbAsync.runWithUi(() -> {
        return authSessionQueryService.logout();
      }, logoutResult -> {
        // Read user state from storage after logout so label/icon are always in sync.
        refreshAuthState();

        if (logoutResult == null) {
          ToastManager.getInstance().showWarning("Authentication", "Signed out locally.");
          return;
        }

        if (logoutResult.reason() == AuthSessionQueryService.LogoutReason.SUCCESS) {
          ToastManager.getInstance().showSuccess("Authentication", "Signed out successfully.");
          return;
        }

        ToastManager.getInstance().showWarning("Authentication", logoutResult.message());
      }, err -> {
        err.printStackTrace();
        refreshAuthState();
        ToastManager.getInstance().showWarning("Authentication", "Signed out locally. Please log in again.");
      });
    });
  }
}
