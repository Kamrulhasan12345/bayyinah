package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.ui.ToastManager;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.Setter;
import org.kordamp.ikonli.javafx.FontIcon;

public class RailNavigationController {

  @FXML
  private StackPane readerBtn;

  @FXML
  private StackPane bookmarksBtn;

  @FXML
  private StackPane progressBtn;

  @FXML
  private StackPane meetingBtn;

  @FXML
  private StackPane peopleBtn;

  @FXML
  private StackPane authBtn;

  @FXML
  private StackPane settingsBtn;

  @FXML
  private FontIcon authIcon;

  @Setter
  private Runnable onReaderClicked;

  @Setter
  private Runnable onBookmarksClicked;

  @Setter
  private Runnable onReadingProgressClicked;

  @Setter
  private Runnable onMeetingClicked;

  @Setter
  private Runnable onSettingsClicked;

  @Setter
  private Runnable onLoginClicked;

  @Setter
  private Runnable onAuthStateChanged;

  @Setter
  private AppContext appContext;

  private Tooltip displayNameTooltip;
  private PauseTransition tooltipHideDelay;

  public void initializeRail() {
    setupClickHandlers();
    setupDisplayNameTooltip();
    refreshAuthState();
  }

  private void setupClickHandlers() {
    readerBtn.setOnMouseClicked(e -> {
      setActiveButton(readerBtn);
      if (onReaderClicked != null) {
        onReaderClicked.run();
      }
    });

    bookmarksBtn.setOnMouseClicked(e -> {
      setActiveButton(bookmarksBtn);
      if (onBookmarksClicked != null) {
        onBookmarksClicked.run();
      }
    });

    progressBtn.setOnMouseClicked(e -> {
      setActiveButton(progressBtn);
      if (onReadingProgressClicked != null) {
        onReadingProgressClicked.run();
      }
    });

    meetingBtn.setOnMouseClicked(e -> {
      setActiveButton(meetingBtn);
      if (onMeetingClicked != null) {
        onMeetingClicked.run();
      }
    });

    settingsBtn.setOnMouseClicked(e -> {
      setActiveButton(settingsBtn);
      if (onSettingsClicked != null) {
        onSettingsClicked.run();
      }
    });
  }

  private void setupDisplayNameTooltip() {
    displayNameTooltip = new Tooltip("Guest User");
    displayNameTooltip.getStyleClass().add("rail-tooltip");
    Tooltip.install(peopleBtn, displayNameTooltip);

    tooltipHideDelay = new PauseTransition(Duration.millis(1200));
    tooltipHideDelay.setOnFinished(e -> displayNameTooltip.hide());

    peopleBtn.focusedProperty().addListener((obs, oldV, focused) -> {
      if (focused) {
        showPeopleTooltip();
      }
    });

    peopleBtn.setOnMouseClicked(e -> showPeopleTooltip());
  }

  private void showPeopleTooltip() {
    if (peopleBtn.getScene() == null || peopleBtn.getScene().getWindow() == null) {
      return;
    }

    double x = peopleBtn.localToScreen(peopleBtn.getBoundsInLocal()).getMaxX() + 8;
    double y = peopleBtn.localToScreen(peopleBtn.getBoundsInLocal()).getMinY() + 4;

    displayNameTooltip.show(peopleBtn, x, y);
    tooltipHideDelay.playFromStart();
  }

  private void setActiveButton(StackPane activeButton) {
    List<StackPane> mainNavButtons = List.of(readerBtn, bookmarksBtn, progressBtn, meetingBtn);
    for (StackPane button : mainNavButtons) {
      if (button == activeButton) {
        if (!button.getStyleClass().contains("rail-button-active")) {
          button.getStyleClass().add("rail-button-active");
        }
      } else {
        button.getStyleClass().remove("rail-button-active");
      }
    }
  }

  public void activateReaderTab() {
    setActiveButton(readerBtn);
  }

  public void refreshAuthState() {
    if (appContext == null) {
      return;
    }

    AuthSessionQueryService authSessionQueryService = appContext.getAuthSessionQueryService();
    if (authSessionQueryService == null) {
      return;
    }

    DbAsync.runWithUi(authSessionQueryService::getCurrentUser, user -> {
      String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Guest User";
      displayNameTooltip.setText(displayName);

      if (user.isGuest()) {
        bindGuestAuthAction();
      } else {
        bindLogoutAction(authSessionQueryService);
      }
    }, e -> {
      e.printStackTrace();
      displayNameTooltip.setText("Guest User");
      bindGuestAuthAction();
      ToastManager.getInstance().showWarning("Authentication", "Unable to refresh auth state. Using guest mode.");
    });
  }

  private void bindGuestAuthAction() {
    authIcon.setIconLiteral("mdi2l-login");
    authBtn.setOnMouseClicked(e -> {
      if (onLoginClicked != null) {
        onLoginClicked.run();
      }
    });
  }

  private void bindLogoutAction(AuthSessionQueryService authSessionQueryService) {
    authIcon.setIconLiteral("mdi2l-logout");
    authBtn.setOnMouseClicked(e -> {
      DbAsync.runWithUi(authSessionQueryService::logout, logoutResult -> {
        if (onAuthStateChanged != null) {
          onAuthStateChanged.run();
        } else {
          refreshAuthState();
        }

        if (logoutResult == null) {
          ToastManager.getInstance().showWarning("Authentication", "Signed out locally.");
          return;
        }

        if (logoutResult.reason() == AuthSessionQueryService.LogoutReason.SUCCESS) {
          ToastManager.getInstance().showSuccess("Authentication", "Signed out successfully.");
          return;
        }

        ToastManager.getInstance().showWarning("Authentication", logoutResult.message());
      }, err -> {
        err.printStackTrace();
        if (onAuthStateChanged != null) {
          onAuthStateChanged.run();
        } else {
          refreshAuthState();
        }
        ToastManager.getInstance().showWarning("Authentication", "Signed out locally. Please log in again.");
      });
    });
  }
}
