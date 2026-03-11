package com.ks.bayyinah.controller;

import com.ks.bayyinah.App;
import com.ks.bayyinah.context.AppContext;

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
  private BrowsingController browsingController;
  // TODO: private Node settingsView;

  private AppContext appContext;

  public void showLoginOverlay() {
    showAuthOverlay("fxml/LoginView.fxml");
  }

  public void showRegistrationOverlay() {
    showAuthOverlay("fxml/RegistrationView.fxml");
  }

  public void showBrowsingView() {
    System.out.println("RootController: showBrowsingView called with AppContext: " + appContext);
    if (browsingView == null) {
      try {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/BrowsingView.fxml"));
        browsingView = loader.load();
        browsingController = loader.getController();
        browsingController.setRootController(this);
        browsingController.setAppContext(appContext);

        browsingController.initializeBrowsingController();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    mainContainer.getChildren().setAll(browsingView);
  }

  private void showAuthOverlay(String fxmlPath) {
    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
      Node authView = loader.load();
      authView.setOnMouseClicked(e -> e.consume());
      
      Object c = loader.getController();
      if (c instanceof LoginController lc) {
        lc.setAppContext(appContext);
        lc.setRootController(this);
      } else if (c instanceof RegistrationController rc) {
        rc.setAppContext(appContext);
        rc.setRootController(this);
      }

      overlay.getChildren().setAll(authView);
      overlay.setVisible(true);
      overlay.setManaged(true);

      overlay.setOnMouseClicked(e -> hideOverlay());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void hideOverlay() {
    overlay.setVisible(false);
    overlay.setManaged(false);
    overlay.getChildren().clear();
  }

  public void onAuthStateChanged() {
    if (browsingController != null) {
      browsingController.refreshAuthUi();
    }
  }
}
