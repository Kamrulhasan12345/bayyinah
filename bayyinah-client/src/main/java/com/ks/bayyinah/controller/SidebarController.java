package com.ks.bayyinah.controller;

import com.ks.bayyinah.controller.cell.ChapterSidebarCell;
import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.query.QuranQueryService;
import com.ks.bayyinah.infra.local.database.DBExecutor;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.Setter;

public class SidebarController {

  @FXML
  private ListView<ChapterView> chaptersListView;

  @FXML
  private TextField searchField;

  @Setter
  private Consumer<ChapterView> onChapterSelected;

  @FXML
  public void initialize() {
    LocalQuranQueryService quranQueryService = new LocalQuranQueryService();

    DBExecutor.run(() -> {
      // Ensure DB is initialized before querying
      List<ChapterView> chaptersData = quranQueryService.getAllChapters("en");
      System.out.println(
          "Fetched " +
              chaptersData.size() +
              " chapters from DB");
      ObservableList<ChapterView> chapters = FXCollections.observableArrayList(
          chaptersData);
      Platform.runLater(() -> chaptersListView.setItems(chapters));
    });

    chaptersListView.setCellFactory(listView -> new ChapterSidebarCell());

    chaptersListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, old, selected) -> {
          if (selected != null) {
            System.out.println(
                "Selected chapter: " + selected.getChapter().getNameSimple());
            onChapterSelected.accept(selected);
          }
        });

    searchField
        .textProperty()
        .addListener((obs, oldText, newText) -> {
          DBExecutor.run(() -> {
            List<ChapterView> filteredData = quranQueryService.searchChapter(
                newText,
                "en");
            ObservableList<ChapterView> filtered = FXCollections.observableArrayList(filteredData);
            Platform.runLater(() -> chaptersListView.setItems(filtered));
          });
        });
  }
}
