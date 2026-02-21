package com.ks.bayyinah.controller;

import com.ks.bayyinah.App;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class RootController {
  @FXML
  private StackPane mainContainer;
  @FXML
  private VBox overlay;
  // TODO: header controller here @FXML private

  private Node browsingView;
  // TODO: private Node settingsView;

  @FXML
  public void initialize() {
    showBrowsingView();
  }

  public void showBrowsingView() {
    if (browsingView == null) {
      try {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/BrowsingView.fxml"));
        browsingView = loader.load();
        BrowsingController browsingController = loader.getController();
        browsingController.setRootController(this);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    mainContainer.getChildren().setAll(browsingView);
  }
}
