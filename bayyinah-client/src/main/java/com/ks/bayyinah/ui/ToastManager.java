package com.ks.bayyinah.ui;

import com.ks.bayyinah.error.ErrorCategory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.Queue;

public class ToastManager {

  private static ToastManager instance;

  private final VBox toastContainer;
  private final Queue<ToastNotification> queue;
  private boolean isShowingToast;

  private ToastManager() {
    this.queue = new LinkedList<>();
    this.isShowingToast = false;

    // Create container for toasts (bottom-right corner)
    toastContainer = new VBox(10);
    toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
    toastContainer.setPadding(new Insets(20));
    toastContainer.setMouseTransparent(false); // Don't block clicks
    toastContainer.setPickOnBounds(false);
  }

  public synchronized static ToastManager getInstance() {
    if (instance == null) {
      instance = new ToastManager();
    }
    return instance;
  }

  /**
   * Initialize with primary stage
   */
  public void initialize(Stage primaryStage) {
    Scene scene = primaryStage.getScene();

    if (scene != null && scene.getRoot() instanceof StackPane) {
      StackPane root = (StackPane) scene.getRoot();

      // Add toast container as overlay
      if (!root.getChildren().contains(toastContainer)) {
        root.getChildren().add(toastContainer);
        StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
      }
    }
  }

  /**
   * Show error toast
   */
  public void showError(String title, String message) {
    showToast(title, message, ToastSeverity.ERROR, null);
  }

  /**
   * Show error toast from ErrorCategory
   */
  public void showError(ErrorCategory category) {
    showToast(category.getTitle(), category.getMessage(), category.getSeverity(), null);
  }

  /**
   * Show warning toast
   */
  public void showWarning(String title, String message) {
    showToast(title, message, ToastSeverity.WARNING, null);
  }

  /**
   * Show info toast
   */
  public void showInfo(String title, String message) {
    showToast(title, message, ToastSeverity.INFO, null);
  }

  /**
   * Show success toast
   */
  public void showSuccess(String title, String message) {
    showToast(title, message, ToastSeverity.SUCCESS, null);
  }

  /**
   * Show debug toast
   */
  public void showDebug(String title, String message) {
    showToast(title, message, ToastSeverity.DEBUG, null);
  }

  /**
   * Show toast notification
   */
  private void showToast(String title, String message, ToastSeverity severity, String iconLiteral) {
    Platform.runLater(() -> {
      ToastNotification toast = new ToastNotification(title, message, severity, iconLiteral);
      toast.enableClickToDismiss();

      toast.setOnDismiss(() -> {
        toastContainer.getChildren().remove(toast);
        isShowingToast = false;
        showNextToast(); // Show next in queue
      });

      // If already showing a toast, queue this one
      if (isShowingToast) {
        queue.offer(toast);
      } else {
        displayToast(toast);
      }
    });
  }

  /**
   * Display toast on screen
   */
  private void displayToast(ToastNotification toast) {
    isShowingToast = true;
    toastContainer.getChildren().add(toast);
    toast.show();
  }

  /**
   * Show next toast from queue
   */
  private void showNextToast() {
    ToastNotification next = queue.poll();
    if (next != null) {
      displayToast(next);
    }
  }
}
