package com.ks.bayyinah.controller;

import com.ks.bayyinah.App;
import com.ks.bayyinah.core.dto.ChapterView;
import java.io.IOException;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
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
import javafx.util.Duration;

public class BrowsingController {

  @FXML
  private SplitPane splitPane;

  @FXML
  private SidebarController sidebarController;

  @FXML
  private StackPane contentArea;

  @FXML
  private AnchorPane sidebarContainer;

  private RootController rootController;
  private int currentShownChapterId;


  private VBox loadingOverlay;

  public void setRootController(RootController rootController) {
    this.rootController = rootController;
  }

  @FXML
  public void initialize() {
    // make the split pane divider fixed at 20%
    setupSidebar();

    createLoadingOverlay();

    showHome();

    sidebarController.setOnHomeBtnClick(this::handleHomeClicked);
    sidebarController.setOnChapterSelected(this::showChapter);
  }

  private void handleHomeClicked() {
    System.out.println("Home button clicked in BrowsingController");
    if (sidebarController != null) {
      sidebarController.clearSelection();
    }
    showHome();
  }

  private void createLoadingOverlay() {
    ProgressIndicator spinner = new ProgressIndicator();
    spinner.setMaxSize(50, 50);

    Label loadingLabel = new Label("Loading...");
    loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

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
    Platform.runLater(() -> {
      loadingOverlay.setVisible(true);
      loadingOverlay.setManaged(true);
      loadingOverlay.toFront();
    });
  }

  private void hideLoading() {
    Platform.runLater(() -> {
      loadingOverlay.setVisible(false);
      loadingOverlay.setManaged(false);
    });
  }

  private void setupSidebar() {
    SplitPane.Divider divider = splitPane.getDividers().get(0);

    divider
      .positionProperty()
      .addListener((observable, oldValue, newValue) -> {
        double clamped =
          newValue.doubleValue() > 0.2 ? 0.2 : newValue.doubleValue();
        divider.setPosition(clamped);
      });
  }

  private void showHome() {
    try {
      FXMLLoader loader = new FXMLLoader(
        App.class.getResource("fxml/HomeView.fxml")
      );
      Node homeView = loader.load();
      HomeController homeController = loader.getController();
      homeController.setBrowsingController(this);
      contentArea.getChildren().setAll(homeView);
      contentArea.getChildren().add(loadingOverlay);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showChapter(ChapterView chapter) {
    if (currentShownChapterId == chapter.getChapter().getId()) {
      System.out.println(
        "Chapter " +
          chapter.getChapter().getId() +
          " is already shown, skipping"
      );
      return;
    }

    showLoading();

    try {
      FXMLLoader loader = new FXMLLoader(
        App.class.getResource("fxml/ChaptersView.fxml")
      );
      Node chaptersView = loader.load();
      ChaptersController chaptersController = loader.getController();

      chaptersController.setOnLoadComplete(() -> hideLoading());
      chaptersController.setChapter(chapter);

      currentShownChapterId = chapter.getChapter().getId();
      chaptersController.setBrowsingController(this);
      contentArea.getChildren().setAll(chaptersView);
      contentArea.getChildren().add(loadingOverlay);
    } catch (IOException e) {
      e.printStackTrace();
      hideLoading();
    }
  }
}
