package com.ks.bayyinah.controller;

import com.ks.bayyinah.App;
import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.hybrid.model.MainConfig;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.hybrid.query.TokenManager;
import com.ks.bayyinah.infra.hybrid.service.*;
import com.ks.bayyinah.infra.remote.client.ApiClient;
import com.ks.bayyinah.infra.remote.query.RemoteUserQueryService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import lombok.Data;

import java.io.IOException;

@Data
public class RootController {
  @FXML
  private StackPane mainContainer;
  @FXML
  private VBox overlay;

  private Node browsingView;
  // TODO: private Node settingsView;

  private AppContext appContext;

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
