package com.ks.bayyinah.controller;

import com.ks.bayyinah.App;
import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.core.dto.ChapterView;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.*;

@Data
public class BrowsingController {

  private enum ContentMode {
    READER,
    MEETING,
    AUXILIARY
  }

  @FXML
  private SplitPane splitPane;

  @FXML
  private SidebarController sidebarController;

  @FXML
  private RailNavigationController railNavigationController;

  @FXML
  private StackPane contentArea;

  @FXML
  private AnchorPane sidebarContainer;

  private RootController rootController;
  private int currentShownChapterId;

  private Boolean partialChapterView;

  private AppContext appContext;

  private VBox loadingOverlay;
  private ContentMode contentMode = ContentMode.READER;
  private MeetingViewController activeMeetingController;
  private ChaptersController activeChaptersController;
  private Node cachedMeetingView;

  public void setRootController(RootController rootController) {
    this.rootController = rootController;
  }

  public void initializeBrowsingController() {
    setupSidebar();
    setContentMode(ContentMode.READER);

    createLoadingOverlay();

    showHome();

    System.out.println("BrowsingController initialized with AppContext: " + appContext);
  }

  private void handleHomeClicked() {
    System.out.println("Home button clicked in BrowsingController");
    if (sidebarController != null) {
      sidebarController.clearSelection();
    }
    if (railNavigationController != null) {
      railNavigationController.activateReaderTab();
    }
    showHome();
  }

  private void createLoadingOverlay() {
    ProgressIndicator spinner = new ProgressIndicator();
    spinner.setMaxSize(50, 50);

    Label loadingLabel = new Label("Loading...");
    loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-font-family: \"Inter\"");

    loadingOverlay = new VBox(15);
    loadingOverlay.setAlignment(Pos.CENTER);
    loadingOverlay.getChildren().addAll(spinner, loadingLabel);
    loadingOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9);");
    loadingOverlay.setVisible(false);
    loadingOverlay.setManaged(false);

    // Add to contentArea as overlay
    contentArea.getChildren().add(loadingOverlay);
  }

  private void showLoading() {
    loadingOverlay.setVisible(true);
    loadingOverlay.setManaged(true);
    loadingOverlay.toFront();
  }

  private void hideLoading() {
    System.out.println("Hiding loading overlay");
    loadingOverlay.setVisible(false);
    loadingOverlay.setManaged(false);
  }

  private void setupSidebar() {
    applySidebarVisibility(true);

    sidebarController.setOnHomeBtnClick(this::handleHomeClicked);
    sidebarController.setOnSettingsClicked(this::showSettings);
    sidebarController.setOnChapterSelected(this::handleSidebarChapterSelection);
    sidebarController.setOnLoginClicked(() -> {
      if (rootController != null) {
        rootController.showLoginOverlay();
      }
    });
    sidebarController.setAppContext(appContext);

    sidebarController.initializeSidebar();

    if (railNavigationController != null) {
      railNavigationController.setOnReaderClicked(this::handleHomeClicked);
      railNavigationController.setOnBookmarksClicked(this::showBookmarks);
      railNavigationController.setOnReadingProgressClicked(this::showReadingProgress);
      railNavigationController.setOnMeetingClicked(this::showMeeting);
      railNavigationController.setOnSettingsClicked(this::showSettings);
      railNavigationController.setOnLoginClicked(() -> {
        if (rootController != null) {
          rootController.showLoginOverlay();
        }
      });
      railNavigationController.setOnAuthStateChanged(this::refreshAuthUi);
      railNavigationController.setAppContext(appContext);
      railNavigationController.initializeRail();
    }
  }

  private void setContentMode(ContentMode mode) {
    contentMode = mode;
    if (sidebarController != null) {
      sidebarController.setMeetingMode(mode == ContentMode.MEETING);
    }
    if (mode != ContentMode.READER) {
      activeChaptersController = null;
    }
  }

  private void handleSidebarChapterSelection(ChapterView chapter) {
    if (chapter == null) {
      return;
    }

    if (contentMode == ContentMode.MEETING) {
      if (activeMeetingController != null) {
        activeMeetingController.onSidebarChapterSelected(chapter);
      }
      return;
    }

    showChapter(chapter);
  }

  public void refreshAuthUi() {
    if (sidebarController != null) {
      sidebarController.refreshAuthState();
    }
    if (railNavigationController != null) {
      railNavigationController.refreshAuthState();
    }
  }

  private void showHome() {
    setContentMode(ContentMode.READER);
    applySidebarVisibility(true);
    try {
      FXMLLoader loader = new FXMLLoader(
          App.class.getResource("fxml/HomeView.fxml"));
      Node homeView = loader.load();
      HomeController homeController = loader.getController();
      homeController.setBrowsingController(this);
      homeController.setAppContext(appContext);
      homeController.initializeHome();
      contentArea.getChildren().setAll(homeView);
      contentArea.getChildren().add(loadingOverlay);
      currentShownChapterId = -1; // reset current chapter since we're on home
      partialChapterView = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showSettings() {
    setContentMode(ContentMode.AUXILIARY);
    applySidebarVisibility(false);
    if (sidebarController != null) {
      sidebarController.clearSelection();
    }
    try {
      FXMLLoader loader = new FXMLLoader(
          App.class.getResource("fxml/SettingsView.fxml"));
      Node settingsView = loader.load();
      SettingsController settingsController = loader.getController();
      settingsController.setAppContext(appContext);
      settingsController.initializeSettings();

      contentArea.getChildren().setAll(settingsView);
      contentArea.getChildren().add(loadingOverlay);
      currentShownChapterId = -1;
      partialChapterView = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showBookmarks() {
    setContentMode(ContentMode.AUXILIARY);
    applySidebarVisibility(false);

    if (sidebarController != null) {
      sidebarController.clearSelection();
    }

    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/BookmarksView.fxml"));
      Node view = loader.load();

      Object controller = loader.getController();
      if (controller instanceof BookmarksController bookmarksController) {
        bookmarksController.setAppContext(appContext);
        bookmarksController.setBrowsingController(this);
        bookmarksController.initializeBookmarks();
      }

      contentArea.getChildren().setAll(view);
      contentArea.getChildren().add(loadingOverlay);
      currentShownChapterId = -1;
      partialChapterView = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showReadingProgress() {
    setContentMode(ContentMode.AUXILIARY);
    applySidebarVisibility(false);

    if (sidebarController != null) {
      sidebarController.clearSelection();
    }

    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/ReadingProgressView.fxml"));
      Node view = loader.load();

      Object controller = loader.getController();
      if (controller instanceof ReadingProgressController readingProgressController) {
        readingProgressController.setAppContext(appContext);
        readingProgressController.setBrowsingController(this);
        readingProgressController.initializeReadingProgress();
      }

      contentArea.getChildren().setAll(view);
      contentArea.getChildren().add(loadingOverlay);
      currentShownChapterId = -1;
      partialChapterView = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showMeeting() {
    setContentMode(ContentMode.MEETING);
    applySidebarVisibility(true);

    if (sidebarController != null) {
      sidebarController.clearSelection();
    }

    try {
      if (cachedMeetingView == null || activeMeetingController == null) {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/MeetingView.fxml"));
        cachedMeetingView = loader.load();

        Object controller = loader.getController();
        if (controller instanceof MeetingViewController meetingViewController) {
          meetingViewController.setAppContext(appContext);
          meetingViewController.initializeMeeting();
          activeMeetingController = meetingViewController;
        }
      } else {
        activeMeetingController.setAppContext(appContext);
      }

      contentArea.getChildren().setAll(cachedMeetingView);
      contentArea.getChildren().add(loadingOverlay);
      currentShownChapterId = -1;
      partialChapterView = true;
    } catch (IOException e) {
      e.printStackTrace();
      cachedMeetingView = null;
      activeMeetingController = null;
      setContentMode(ContentMode.READER);
    }
  }

  private void loadSimpleView(String fxmlPath, boolean showSidebar) {
    setContentMode(ContentMode.AUXILIARY);
    applySidebarVisibility(showSidebar);

    if (sidebarController != null) {
      sidebarController.clearSelection();
    }

    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
      Node view = loader.load();

      contentArea.getChildren().setAll(view);
      contentArea.getChildren().add(loadingOverlay);
      currentShownChapterId = -1;
      partialChapterView = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void showChapter(ChapterView chapter) {
    showChapter(chapter, null, null, null);
  }

  public void showChapterAndFocusAyah(ChapterView chapter, Integer focusAyah) {
    showChapter(chapter, null, null, null, focusAyah);
  }

  public void showChapter(ChapterView chapter, Integer startVerse, Integer endVerse, Integer translationId) {
    showChapter(chapter, startVerse, endVerse, translationId, null);
  }

  private void showChapter(ChapterView chapter, Integer startVerse, Integer endVerse, Integer translationId,
      Integer focusAyah) {
    if (chapter == null || chapter.getChapter() == null) {
      return;
    }

    setContentMode(ContentMode.READER);

    boolean fullChapterRequest = startVerse == null && endVerse == null;

    if (contentMode == ContentMode.READER && currentShownChapterId == chapter.getChapter().getId()
        && Boolean.FALSE.equals(partialChapterView) && fullChapterRequest && translationId == null
        && focusAyah != null && focusAyah > 0 && activeChaptersController != null) {
      activeChaptersController.focusAyah(focusAyah);
      if (railNavigationController != null) {
        railNavigationController.activateReaderTab();
      }
      return;
    }

    if (currentShownChapterId == chapter.getChapter().getId()
        && Boolean.FALSE.equals(partialChapterView)
        && contentMode == ContentMode.READER
      && activeChaptersController != null
        && fullChapterRequest
        && focusAyah == null
        && translationId == null) {
      System.out.println(
          "Chapter " +
              chapter.getChapter().getId() +
              " is already shown, skipping");
      return;
    }

    showLoading();
    applySidebarVisibility(true);

    if (railNavigationController != null) {
      railNavigationController.activateReaderTab();
    }

    try {
      FXMLLoader loader = new FXMLLoader(
          App.class.getResource("fxml/ChaptersView.fxml"));
      Node chaptersView = loader.load();
      ChaptersController chaptersController = loader.getController();

      chaptersController.setOnLoadComplete(() -> hideLoading());
      chaptersController.setAppContext(appContext);
      chaptersController.setChapter(chapter, startVerse, endVerse, translationId);
      if (focusAyah != null && focusAyah > 0) {
        chaptersController.focusAyah(focusAyah);
      }
      activeChaptersController = chaptersController;

      currentShownChapterId = chapter.getChapter().getId();

      if (startVerse != null && endVerse != null) {
        partialChapterView = true;
      } else {
        partialChapterView = false;
      }

      chaptersController.setBrowsingController(this);

      contentArea.getChildren().setAll(chaptersView);
      contentArea.getChildren().add(loadingOverlay);

    } catch (IOException e) {
      e.printStackTrace();
      hideLoading();
    }
  }

  private void applySidebarVisibility(boolean showSidebar) {
    if (sidebarContainer == null || splitPane == null) {
      return;
    }

    sidebarContainer.setVisible(showSidebar);
    sidebarContainer.setManaged(showSidebar);

    if (showSidebar) {
      sidebarContainer.setMinWidth(0);
      sidebarContainer.setPrefWidth(260);
      sidebarContainer.setMaxWidth(220);
      splitPane.setDividerPosition(0, 0.22);
      return;
    }

    sidebarContainer.setMinWidth(0);
    sidebarContainer.setPrefWidth(0);
    sidebarContainer.setMaxWidth(0);
    splitPane.setDividerPosition(0, 0);
  }
}
