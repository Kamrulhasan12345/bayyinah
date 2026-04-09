package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.error.ErrorCategory;
import com.ks.bayyinah.error.ErrorMapper;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.ui.ToastManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.Setter;

public class LoginController {

  private static final String INVALID_STYLE_CLASS = "auth-input-invalid";

  @FXML
  private Button loginBtn;

  @FXML
  private PasswordField passwordField;

  @FXML
  private Hyperlink registerRedirect;

  @FXML
  private Label formMessageLabel;

  @FXML
  private TextField usernameField;

  @Setter
  private AppContext appContext;

  private RootController rootController;
  private boolean submitting;
  private String defaultLoginButtonText = "Login";

  @FXML
  private void initialize() {
    if (loginBtn != null) {
      defaultLoginButtonText = loginBtn.getText();
    }
    clearValidationState();
  }

  public void login() {
    if (submitting) {
      return;
    }

    String validationMessage = validateForm();
    if (validationMessage != null) {
      showValidationMessage(validationMessage);
      ToastManager.getInstance().showWarning("Login", validationMessage);
      return;
    }

    if (appContext == null || appContext.getAuthSessionQueryService() == null) {
      String message = "Authentication service is unavailable. Please restart the app.";
      showValidationMessage(message);
      ToastManager.getInstance().showError("Login", message);
      return;
    }

    AuthSessionQueryService authSessionQueryService = appContext.getAuthSessionQueryService();
    String username = usernameField.getText().trim();
    String password = passwordField.getText();

    setSubmitting(true);

    DbAsync.runWithUi(() -> {
      authSessionQueryService.login(username, password);
      return null;
    }, ignored -> {
      setSubmitting(false);
      clearValidationState();
      if (rootController != null) {
        rootController.onAuthStateChanged();
        rootController.hideOverlay();
      }
      ToastManager.getInstance().showSuccess("Login", "Logged in successfully.");
    }, err -> {
      setSubmitting(false);
      ErrorCategory category = ErrorMapper.mapException(err);
      showValidationMessage(category.getMessage());
      ToastManager.getInstance().showError(category);
    });
  }

  public void setRootController(RootController rootController) {
    this.rootController = rootController;
  }

  public void goToRegister() {
    if (submitting) {
      return;
    }

    if (rootController != null) {
      rootController.hideOverlay();
      rootController.showRegistrationOverlay();
    }
  }

  private String validateForm() {
    clearValidationState();

    boolean missingUsername = usernameField == null
        || usernameField.getText() == null
        || usernameField.getText().trim().isEmpty();
    boolean missingPassword = passwordField == null
        || passwordField.getText() == null
        || passwordField.getText().isEmpty();

    if (missingUsername) {
      markInvalid(usernameField);
    }
    if (missingPassword) {
      markInvalid(passwordField);
    }

    if (missingUsername && missingPassword) {
      return "Please enter your username and password.";
    }
    if (missingUsername) {
      return "Please enter your username.";
    }
    if (missingPassword) {
      return "Please enter your password.";
    }

    return null;
  }

  private void setSubmitting(boolean submitting) {
    this.submitting = submitting;

    if (loginBtn != null) {
      loginBtn.setDisable(submitting);
      loginBtn.setText(submitting ? "Logging in..." : defaultLoginButtonText);
    }

    if (usernameField != null) {
      usernameField.setDisable(submitting);
    }
    if (passwordField != null) {
      passwordField.setDisable(submitting);
    }
    if (registerRedirect != null) {
      registerRedirect.setDisable(submitting);
    }
  }

  private void clearValidationState() {
    clearInvalid(usernameField);
    clearInvalid(passwordField);
    hideValidationMessage();
  }

  private void markInvalid(TextField field) {
    if (field == null || field.getStyleClass().contains(INVALID_STYLE_CLASS)) {
      return;
    }
    field.getStyleClass().add(INVALID_STYLE_CLASS);
  }

  private void clearInvalid(TextField field) {
    if (field == null) {
      return;
    }
    field.getStyleClass().remove(INVALID_STYLE_CLASS);
  }

  private void showValidationMessage(String message) {
    if (formMessageLabel == null) {
      return;
    }
    formMessageLabel.setText(message == null ? "" : message);
    formMessageLabel.setManaged(true);
    formMessageLabel.setVisible(true);
  }

  private void hideValidationMessage() {
    if (formMessageLabel == null) {
      return;
    }
    formMessageLabel.setText("");
    formMessageLabel.setManaged(false);
    formMessageLabel.setVisible(false);
  }
}
