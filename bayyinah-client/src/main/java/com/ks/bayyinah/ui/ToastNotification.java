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
  private static final double TOAST_HEIGHT = 80;

  private final Label titleLabel;
  private final Label messageLabel;
  private final Timeline autoHideTimeline;
  private Runnable onDismiss;

  public ToastNotification(String title, String message, ToastSeverity severity, String iconLiteral) {
    // Container styling
    // Remove fixed height constraints
    this.setPrefWidth(TOAST_WIDTH);
    this.setMinWidth(TOAST_WIDTH);
    this.setMaxWidth(TOAST_WIDTH);

    // Let height grow with content
    this.setPrefHeight(Region.USE_COMPUTED_SIZE);
    this.setMinHeight(Region.USE_PREF_SIZE); // Or 60 for minimum
    this.setMaxHeight(Double.POSITIVE_INFINITY); // Allow infinite growth
    this.setPadding(new Insets(15));
    this.setSpacing(5);
    this.setAlignment(Pos.CENTER_LEFT);

    // Apply severity styling
    applySeverityStyle(severity);

    // Title
    titleLabel = new Label(title);
    titleLabel.setStyle(
        "-fx-font-weight: bold; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: #000000;");

    HBox.setHgrow(titleLabel, Priority.ALWAYS);

    // Message
    messageLabel = new Label(message);
    messageLabel.setWrapText(true);
    messageLabel.setStyle(
        "-fx-font-size: 12px; " +
            "-fx-text-fill: #000000; " +
            "-fx-opacity: 0.9;");

    // Icon (optional)
    FontIcon icon = new FontIcon(iconLiteral != null ? iconLiteral : getIconForSeverity(severity));

    FontIcon crossIcon = new FontIcon("mdi2c-close:16:#9E9E9E");

    crossIcon.setOnMouseClicked(e -> hide());
    crossIcon.setStyle(crossIcon.getStyle() + "-fx-cursor: hand;");
    crossIcon.setTranslateX(10); // Add some spacing from the right edge
    crossIcon.setTranslateY(-10); // Position it at the top-right corner
    crossIcon.setOpacity(0.7); // Slightly faded for better aesthetics
    crossIcon.setOnMouseEntered(e -> crossIcon.setOpacity(1.0)); // Highlight on hover
    crossIcon.setOnMouseExited(e -> crossIcon.setOpacity(0.7)); // Restore opacity when not hovered
    crossIcon.setPickOnBounds(true); // Allow clicking on transparent areas of the icon
    crossIcon.setFocusTraversable(false); // Don't allow focus on the close icon

    // Layout
    HBox header = new HBox(10, icon, titleLabel, crossIcon);

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
    String baseStyle = "-fx-background-radius: 8px; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2); ";

    this.setStyle(baseStyle + "-fx-background-color: #FFFFFF;"); // Base style

    /*
     * switch (severity) {
     * case INFO:
     * this.setStyle(baseStyle + "-fx-background-color: #2196F3;");
     * break;
     * case WARNING:
     * this.setStyle(baseStyle + "-fx-background-color: #FF9800;");
     * break;
     * case ERROR:
     * this.setStyle(baseStyle + "-fx-background-color: #F44336;");
     * break;
     * case CRITICAL:
     * this.setStyle(baseStyle + "-fx-background-color: #B71C1C;");
     * break;
     * }
     */
  }

  /**
   * Get icon emoji for severity
   */
  private String getIconForSeverity(ToastSeverity severity) {
    switch (severity) {
      case INFO:
        return "mdi2i-information-outline:20:#2196F3";
      case SUCCESS:
        return "mdi2c-check-circle-outline:20:#4CAF50";
      case WARNING:
        return "mdi2a-alert-outline:20:#FF9800";
      case ERROR:
        return "mdi2c-close-circle-outline:20:#F44336";
      case CRITICAL:
        return "mdi2a-alert-octagram:20:#B71C1C";
      case DEBUG:
        return "mdi2b-bug-outline:20:#9E9E9E";
      default:
        return "mdi2h-help-circle-outline:20:#9E9E9E";
    }
  }

  /**
   * Show with slide-in animation
   */
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
    // Stop auto-hide timer
    autoHideTimeline.stop();

    // Slide down and fade out
    TranslateTransition slide = new TranslateTransition(Duration.millis(250), this);
    slide.setToY(100);

    FadeTransition fade = new FadeTransition(Duration.millis(250), this);
    fade.setToValue(0);

    ParallelTransition animation = new ParallelTransition(slide, fade);
    animation.setOnFinished(e -> {
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
    this.setStyle(this.getStyle() + "-fx-cursor: hand;");
  }
}
