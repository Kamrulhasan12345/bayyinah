package com.ks.bayyinah.controller;

import com.ks.bayyinah.App;
import com.ks.bayyinah.core.dto.ChapterView;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.beans.value.ObservableValue;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
// import javafx.scene.layout.Pane;
// import javafx.scene.layout.Region;
// import javafx.scene.layout.Priority;

public class BrowsingController {

  @FXML
  private SplitPane splitPane;

  @FXML
  private SidebarController sidebarController;

  // TODO: @FXML private HeaderController headerController;
  @FXML
  private StackPane contentArea;

  private RootController rootController;
  private int currentShownChapterId;

  private VBox loadingOverlay;

  public void setRootController(RootController rootController) {
    this.rootController = rootController;
    // TODO: this.headerController.setRootController(rootController);
  }

  @FXML
  public void initialize() {
    // make the split pane divider fixed at 20%
    SplitPane.Divider divider = splitPane.getDividers().get(0);

    divider.positionProperty().addListener(
        (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
          divider.setPosition(0.2);
        });
    ;

    createLoadingOverlay();

    showHome();

    // TODO: sidebarController
    sidebarController.setOnChapterSelected(this::showChapter);
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

  private void showHome() {
    try {
      FXMLLoader loader = new FXMLLoader(
          App.class.getResource("fxml/HomeView.fxml"));
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
          "Chapter " + chapter.getChapter().getId() + " is already shown, skipping");
      return;
    }

    showLoading();

    try {
      FXMLLoader loader = new FXMLLoader(
          App.class.getResource("fxml/ChaptersView.fxml"));
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
