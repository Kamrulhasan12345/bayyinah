package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.local.database.DbAsync;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.Setter;

public class RegistrationController {

  @FXML
  private TextField emailField;

  @FXML
  private TextField firstNameField;

  @FXML
  private TextField lastNameField;

  @FXML
  private Label loginRedirect;

  @FXML
  private TextField passwordField;

  @FXML
  private Button registerBtn;

  @FXML
  private TextField usernameField;

  @Setter
  private AppContext appContext;

  private RootController rootController;

  public void register() {
    System.out
        .println("Registering user with username: " + usernameField.getText() + ", email: " + emailField.getText());

    if (usernameField.getText().trim().isEmpty() || passwordField.getText().isEmpty()
        || emailField.getText().trim().isEmpty()) {
      System.out.println("Please fill in all required fields.");
      return;
    }

    AuthSessionQueryService authSessionQueryService = appContext.getAuthSessionQueryService();

    DbAsync.runWithUi(() -> {
      authSessionQueryService.register(
          usernameField.getText().trim(),
          emailField.getText().trim(),
          passwordField.getText(),
          firstNameField.getText().trim(),
          lastNameField.getText().trim());
      return null;
    }, ignored -> {
      if (rootController != null) {
        rootController.onAuthStateChanged();
        rootController.hideOverlay();
      }
    }, err -> {
      err.printStackTrace();
    });
  }

  public void setRootController(RootController rootController) {
    this.rootController = rootController;
  }

  public void goToLogin() {
    if (rootController != null) {
      rootController.hideOverlay();
      rootController.showLoginOverlay();
    }
  }

}
