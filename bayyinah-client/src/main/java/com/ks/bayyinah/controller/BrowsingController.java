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

public class BrowsingController {

  @FXML
  private SplitPane splitPane;

  @FXML
  private SidebarController sidebarController;

  // TODO: @FXML private HeaderController headerController;
  @FXML
  private StackPane contentArea;

  private RootController rootController;

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

    showHome();

    // TODO: sidebarController
    sidebarController.setOnChapterSelected(this::showChapter);
  }

  private void showHome() {
    try {
      FXMLLoader loader = new FXMLLoader(
          App.class.getResource("fxml/HomeView.fxml"));
      Node homeView = loader.load();
      HomeController homeController = loader.getController();
      homeController.setBrowsingController(this);
      contentArea.getChildren().setAll(homeView);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showChapter(ChapterView chapter) {
    try {
      FXMLLoader loader = new FXMLLoader(
          App.class.getResource("fxml/ChaptersView.fxml"));
      Node chaptersView = loader.load();
      ChaptersController chaptersController = loader.getController();
      chaptersController.setChapter(chapter);
      chaptersController.setBrowsingController(this);
      contentArea.getChildren().setAll(chaptersView);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
