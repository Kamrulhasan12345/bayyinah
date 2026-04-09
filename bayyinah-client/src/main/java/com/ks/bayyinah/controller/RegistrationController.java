package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.error.ErrorCategory;
import com.ks.bayyinah.error.ErrorMapper;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.ui.ToastManager;

import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.Setter;

public class RegistrationController {

  private static final String INVALID_STYLE_CLASS = "auth-input-invalid";
  private static final int MIN_PASSWORD_LENGTH = 8;
  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
      Pattern.CASE_INSENSITIVE);

  @FXML
  private TextField emailField;

  @FXML
  private TextField firstNameField;

  @FXML
  private TextField lastNameField;

  @FXML
  private Hyperlink loginRedirect;

  @FXML
  private PasswordField passwordField;

  @FXML
  private Label formMessageLabel;

  @FXML
  private Button registerBtn;

  @FXML
  private TextField usernameField;

  @Setter
  private AppContext appContext;

  private RootController rootController;
  private boolean submitting;
  private String defaultRegisterButtonText = "Register";

  @FXML
  private void initialize() {
    if (registerBtn != null) {
      defaultRegisterButtonText = registerBtn.getText();
    }
    clearValidationState();
  }

  public void register() {
    if (submitting) {
      return;
    }

    String validationMessage = validateForm();
    if (validationMessage != null) {
      showValidationMessage(validationMessage);
      ToastManager.getInstance().showWarning("Registration", validationMessage);
      return;
    }

    if (appContext == null || appContext.getAuthSessionQueryService() == null) {
      String message = "Authentication service is unavailable. Please restart the app.";
      showValidationMessage(message);
      ToastManager.getInstance().showError("Registration", message);
      return;
    }

    AuthSessionQueryService authSessionQueryService = appContext.getAuthSessionQueryService();
    String username = usernameField.getText().trim();
    String email = emailField.getText().trim();
    String password = passwordField.getText();
    String firstName = safeTrim(firstNameField);
    String lastName = safeTrim(lastNameField);

    setSubmitting(true);

    DbAsync.runWithUi(() -> {
      authSessionQueryService.register(
          username,
          email,
          password,
          firstName,
          lastName);
      return null;
    }, ignored -> {
      setSubmitting(false);
      clearValidationState();
      if (rootController != null) {
        rootController.onAuthStateChanged();
        rootController.hideOverlay();
      }
      ToastManager.getInstance().showSuccess("Registration", "Account created and logged in successfully.");
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

  public void goToLogin() {
    if (submitting) {
      return;
    }

    if (rootController != null) {
      rootController.hideOverlay();
      rootController.showLoginOverlay();
    }
  }

  private String validateForm() {
    clearValidationState();

    String username = safeTrim(usernameField);
    String email = safeTrim(emailField);
    String password = passwordField == null || passwordField.getText() == null ? "" : passwordField.getText();

    boolean missingUsername = username.isEmpty();
    boolean missingEmail = email.isEmpty();
    boolean missingPassword = password.isEmpty();

    if (missingUsername) {
      markInvalid(usernameField);
    }
    if (missingEmail) {
      markInvalid(emailField);
    }
    if (missingPassword) {
      markInvalid(passwordField);
    }

    if (missingUsername || missingEmail || missingPassword) {
      return "Please provide username, email, and password.";
    }

    if (!USERNAME_PATTERN.matcher(username).matches()) {
      markInvalid(usernameField);
      return "Username must be 3-30 characters using letters, numbers, '.', '_' or '-'.";
    }

    if (!EMAIL_PATTERN.matcher(email).matches()) {
      markInvalid(emailField);
      return "Please enter a valid email address.";
    }

    if (password.length() < MIN_PASSWORD_LENGTH) {
      markInvalid(passwordField);
      return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.";
    }

    return null;
  }

  private void setSubmitting(boolean submitting) {
    this.submitting = submitting;

    if (registerBtn != null) {
      registerBtn.setDisable(submitting);
      registerBtn.setText(submitting ? "Creating account..." : defaultRegisterButtonText);
    }

    if (usernameField != null) {
      usernameField.setDisable(submitting);
    }
    if (emailField != null) {
      emailField.setDisable(submitting);
    }
    if (firstNameField != null) {
      firstNameField.setDisable(submitting);
    }
    if (lastNameField != null) {
      lastNameField.setDisable(submitting);
    }
    if (passwordField != null) {
      passwordField.setDisable(submitting);
    }
    if (loginRedirect != null) {
      loginRedirect.setDisable(submitting);
    }
  }

  private void clearValidationState() {
    clearInvalid(usernameField);
    clearInvalid(emailField);
    clearInvalid(firstNameField);
    clearInvalid(lastNameField);
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

  private String safeTrim(TextField field) {
    if (field == null || field.getText() == null) {
      return "";
    }
    return field.getText().trim();
  }

}
