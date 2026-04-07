package com.ks.bayyinah.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class ToastNotification extends VBox {

  private static final double TOAST_WIDTH = 350;
  private static final String CLASS_TOAST_PLAIN = "toast-plain";
  private static final String CLASS_TOAST_COLORED = "toast-colored";
  private static final String CLASS_TOAST_INFO = "toast-info";
  private static final String CLASS_TOAST_SUCCESS = "toast-success";
  private static final String CLASS_TOAST_WARNING = "toast-warning";
  private static final String CLASS_TOAST_ERROR = "toast-error";
  private static final String CLASS_TOAST_CRITICAL = "toast-critical";
  private static final String CLASS_TOAST_DEBUG = "toast-debug";

  private final Label titleLabel;
  private final Label messageLabel;
  private final Timeline autoHideTimeline;
  private Runnable onDismiss;

  private final Boolean showBackgroundColor;

  public ToastNotification(String title, String message, ToastSeverity severity, String iconLiteral,
      Boolean showBackgroundColor) {
    // Container styling
    // Remove fixed height constraints
    this.setPrefWidth(TOAST_WIDTH);
    this.setMinWidth(Region.USE_COMPUTED_SIZE);
    this.setMaxWidth(TOAST_WIDTH);

    // Let height grow with content
    this.setPrefHeight(Region.USE_COMPUTED_SIZE);
    this.setMinHeight(Region.USE_PREF_SIZE); // Or 60 for minimum
    this.setMaxHeight(Double.POSITIVE_INFINITY); // Allow infinite growth
    this.setPadding(new Insets(15));
    this.setSpacing(5);
    this.setAlignment(Pos.CENTER_LEFT);
    this.getStyleClass().add("toast-notification");

    this.showBackgroundColor = showBackgroundColor != null ? showBackgroundColor : true;

    // Apply severity styling
    applySeverityStyle(severity);

    titleLabel = new Label(title);
    titleLabel.getStyleClass().add("toast-title");
    titleLabel.getStyleClass().add(this.showBackgroundColor ? "toast-title-on-accent" : "toast-title-plain");

    HBox.setHgrow(titleLabel, Priority.ALWAYS);

    // Message
    messageLabel = new Label(message);
    messageLabel.setWrapText(true);
    messageLabel.getStyleClass().add("toast-message");
    messageLabel.getStyleClass().add(this.showBackgroundColor ? "toast-message-on-accent" : "toast-message-plain");

    // Icon (optional)
    FontIcon icon = new FontIcon(iconLiteral != null ? iconLiteral : getIconForSeverity(severity));
    if (iconLiteral == null) {
      icon.getStyleClass().add(this.showBackgroundColor ? "toast-icon-on-accent" : getPlainIconStyleClass(severity));
    }

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    FontIcon crossIcon = new FontIcon("mdi2c-close:16");
    crossIcon.getStyleClass().add(this.showBackgroundColor ? "toast-close-icon-on-accent" : "toast-close-icon");
    crossIcon.getStyleClass().add("toast-close-icon-button");

    crossIcon.setOnMouseClicked(e -> hide());
    crossIcon.setOpacity(0.7); // Slightly faded for better aesthetics
    crossIcon.setOnMouseEntered(e -> crossIcon.setOpacity(1.0)); // Highlight on hover
    crossIcon.setOnMouseExited(e -> crossIcon.setOpacity(0.7)); // Restore opacity when not hovered
    crossIcon.setPickOnBounds(true); // Allow clicking on transparent areas of the icon
    crossIcon.setFocusTraversable(false); // Don't allow focus on the close icon

    // Layout
    HBox header = new HBox(10, icon, titleLabel, spacer, crossIcon);

    header.setAlignment(Pos.CENTER_LEFT);
    header.setMaxWidth(Double.MAX_VALUE);

    this.getChildren().addAll(header, messageLabel);

    // Auto-hide after 5 seconds
    autoHideTimeline = new Timeline(new KeyFrame(
        Duration.seconds(5),
        e -> hide()));
  }

  /**
   * Apply style based on severity
   */
  private void applySeverityStyle(ToastSeverity severity) {
    this.getStyleClass().removeAll(
        CLASS_TOAST_PLAIN,
        CLASS_TOAST_COLORED,
        CLASS_TOAST_INFO,
        CLASS_TOAST_SUCCESS,
        CLASS_TOAST_WARNING,
        CLASS_TOAST_ERROR,
        CLASS_TOAST_CRITICAL,
        CLASS_TOAST_DEBUG);

    if (!showBackgroundColor) {
      this.getStyleClass().add(CLASS_TOAST_PLAIN);
      return;
    }

    this.getStyleClass().add(CLASS_TOAST_COLORED);
    this.getStyleClass().add(getColoredSeverityStyleClass(severity));
  }

  private String getColoredSeverityStyleClass(ToastSeverity severity) {
    switch (severity) {
      case INFO:
        return CLASS_TOAST_INFO;
      case SUCCESS:
        return CLASS_TOAST_SUCCESS;
      case WARNING:
        return CLASS_TOAST_WARNING;
      case ERROR:
        return CLASS_TOAST_ERROR;
      case CRITICAL:
        return CLASS_TOAST_CRITICAL;
      case DEBUG:
      default:
        return CLASS_TOAST_DEBUG;
    }
  }

  private String getPlainIconStyleClass(ToastSeverity severity) {
    switch (severity) {
      case INFO:
        return "toast-icon-info";
      case SUCCESS:
        return "toast-icon-success";
      case WARNING:
        return "toast-icon-warning";
      case ERROR:
        return "toast-icon-error";
      case CRITICAL:
        return "toast-icon-critical";
      case DEBUG:
      default:
        return "toast-icon-debug";
    }
  }

  /**
   * Get icon emoji for severity
   */
  private String getIconForSeverity(ToastSeverity severity) {
    switch (severity) {
      case INFO:
        return "mdi2i-information-outline:20";
      case SUCCESS:
        return "mdi2c-check-circle-outline:20";
      case WARNING:
        return "mdi2a-alert-outline:20";
      case ERROR:
        return "mdi2c-close-circle-outline:20";
      case CRITICAL:
        return "mdi2a-alert-octagram:20";
      case DEBUG:
        return "mdi2b-bug-outline:20";
      default:
        return "mdi2h-help-circle-outline:20";
    }
  }

  /**
   * Show with slide-in animation
   */
  private boolean isHiding = false;

  public void show() {
    // Start from below the screen
    this.setTranslateY(100);
    this.setOpacity(0);

    // Slide up and fade in
    TranslateTransition slide = new TranslateTransition(Duration.millis(300), this);
    slide.setToY(0);

    FadeTransition fade = new FadeTransition(Duration.millis(300), this);
    fade.setToValue(1.0);

    ParallelTransition animation = new ParallelTransition(slide, fade);
    animation.play();

    // Start auto-hide timer
    autoHideTimeline.play();
  }

  /**
   * Hide with slide-out animation
   */
  public void hide() {
    if (isHiding) {
      return;
    }
    isHiding = true;

    // Stop auto-hide timer
    autoHideTimeline.stop();

    // Slide down and fade out
    TranslateTransition slide = new TranslateTransition(Duration.millis(250), this);
    slide.setToY(100);

    FadeTransition fade = new FadeTransition(Duration.millis(250), this);
    fade.setToValue(0);

    ParallelTransition animation = new ParallelTransition(slide, fade);
    animation.setOnFinished(e -> {
      isHiding = false;
      if (onDismiss != null) {
        onDismiss.run();
      }
    });
    animation.play();
  }

  /**
   * Set callback when dismissed
   */
  public void setOnDismiss(Runnable callback) {
    this.onDismiss = callback;
  }

  /**
   * Allow manual dismissal (click to dismiss)
   */
  public void enableClickToDismiss() {
    this.setOnMouseClicked(e -> hide());
    this.getStyleClass().add("toast-clickable");
  }
}
