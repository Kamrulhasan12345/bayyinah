package com.ks.bayyinah.controller;

import com.ks.bayyinah.controller.cell.ChapterSidebarCell;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.infra.local.database.DBExecutor;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import lombok.Setter;
import org.kordamp.ikonli.javafx.FontIcon;

public class SidebarController {

  @FXML
  private ListView<ChapterView> chaptersListView;

  @FXML
  private TextField searchField;

  @FXML
  private FontIcon homeBtn;

  @FXML
  private FontIcon settingsBtn;

  @FXML
  private Label username;

  @Setter
  private Consumer<ChapterView> onChapterSelected;

  @Setter
  private Runnable onHomeBtnClick;

  private List<ChapterView> allChapters;
  private PauseTransition searchDebounce;
  private int currentlySelectedChapterId;

  @FXML
  public void initialize() {
    LocalQuranQueryService quranQueryService =
      LocalQuranQueryService.getInstance();

    searchDebounce = new PauseTransition(Duration.millis(300));

    setupHomeButton();

    DBExecutor.run(() -> {
      // Ensure DB is initialized before querying
      allChapters = quranQueryService.getAllChapters("en");
      System.out.println("Fetched " + allChapters.size() + " chapters from DB");

      ObservableList<ChapterView> chapters = FXCollections.observableArrayList(
        allChapters
      );
      Platform.runLater(() -> chaptersListView.setItems(chapters));
    });

    chaptersListView.setCellFactory(listView -> new ChapterSidebarCell());

    chaptersListView
      .getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, old, selected) -> {
        if (
          selected != null &&
          selected.getChapter().getId() != currentlySelectedChapterId
        ) {
          System.out.println(
            "Selected chapter: " + selected.getChapter().getNameSimple()
          );
          onChapterSelected.accept(selected);
        }
      });

    searchField
      .textProperty()
      .addListener((obs, oldText, newText) -> {
        searchDebounce.setOnFinished(event -> filterChapters(newText));
        searchDebounce.playFromStart(); // Reset timer on each keystroke
      });
  }

  private void filterChapters(String keyword) {
    if (allChapters == null) {
      return;
    }

    List<ChapterView> filtered;
    if (keyword == null || keyword.isBlank()) {
      filtered = allChapters;
    } else {
      String lowerKeyword = keyword.toLowerCase();
      filtered = allChapters
        .stream()
        .filter(cv -> {
          String nameSimple = cv.getChapter().getNameSimple();
          String nameArabic = cv.getChapter().getNameArabic();
          return (
            (nameSimple != null &&
              nameSimple.toLowerCase().contains(lowerKeyword)) ||
            (nameArabic != null && nameArabic.contains(keyword))
          );
        })
        .collect(Collectors.toList());
    }

    ObservableList<ChapterView> filteredList =
      FXCollections.observableArrayList(filtered);
    chaptersListView.setItems(filteredList); // Already on JavaFX thread
  }

  private void setupHomeButton() {
    if (homeBtn != null) {
      homeBtn.setOnMouseClicked(e -> {
        System.out.println("Home button clicked");
        clearSelection();
        if (onHomeBtnClick != null) {
          onHomeBtnClick.run();
        }
      });

      // Hover effect
      // homeBtn.setOnMouseEntered(e ->
      //   homeBtn.setStyle("-fx-cursor: hand; -fx-opacity: 0.7;")
      // );
      // homeBtn.setOnMouseExited(e -> homeBtn.setStyle("-fx-opacity: 1.0;"));
    }
  }

  public void clearSelection() {
    Platform.runLater(() -> {
      chaptersListView.getSelectionModel().clearSelection();
      currentlySelectedChapterId = -1;
    });
  }
}
