package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.local.database.DbAsync;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import lombok.Setter;

public class LoginController {

  @FXML
  private Button loginBtn;

  @FXML
  private TextField passwordField;

  @FXML
  private Hyperlink registerRedirect;

  @FXML
  private TextField usernameField;

  @Setter
  private AppContext appContext;

  private RootController rootController;

  public void login() {
    if (usernameField.getText().trim().isEmpty() || passwordField.getText().isEmpty()) {
      System.out.println("Please enter both username and password.");
      return;
    }

    AuthSessionQueryService authSessionQueryService = appContext.getAuthSessionQueryService();

    DbAsync.runWithUi(() -> {
      authSessionQueryService.login(usernameField.getText().trim(), passwordField.getText());
      return null;
    }, ignored -> {
      rootController.onAuthStateChanged();
        rootController.hideOverlay();
    }, err -> {
      err.printStackTrace();
    });
  }

  public void setRootController(RootController rootController) {
    this.rootController = rootController;
  }

  public void goToRegister() {
    if (rootController != null) {
      rootController.hideOverlay();
      rootController.showRegistrationOverlay();
    }
  }
}
