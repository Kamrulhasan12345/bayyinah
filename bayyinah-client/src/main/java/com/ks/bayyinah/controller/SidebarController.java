package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.core.query.QuranQueryService;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.controller.cell.ChapterSidebarCell;
import com.ks.bayyinah.infra.local.database.DBExecutor;
import com.ks.bayyinah.infra.local.database.DatabaseManager;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.collections.FXCollections;
import javafx.application.Platform;

import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;

public class SidebarController {

  @FXML
  private ListView<ChapterView> chaptersListView;

  @FXML
  private TextField searchField;

  @Setter
  private Consumer<ChapterView> onChapterSelected;

  @FXML
  public void initialize() {
    QuranQueryService quranQueryService = new LocalQuranQueryService();

    DBExecutor.run(() -> {
      DatabaseManager.initialize(System.getProperty("user.home") + "/.bayyinah/quran.db");
      // Ensure DB is initialized before querying
      List<ChapterView> chaptersData = quranQueryService.getAllChapters("en");
      System.out.println(
          "Fetched " + chaptersData.size() + " chapters from DB" + chaptersData.get(0).getChapter().getNameSimple());
      ObservableList<ChapterView> chapters = FXCollections.observableArrayList(chaptersData);
      Platform.runLater(() -> chaptersListView.setItems(chapters));
    });

    chaptersListView.setCellFactory(listView -> new ChapterSidebarCell());

    chaptersListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
      if (selected != null) {
        System.out.println("Selected chapter: " + selected.getChapter().getNameSimple());
        onChapterSelected.accept(selected);
      }
    });

    // TODO:
    // searchField.textProperty().addListener((obs, old, newText) -> { /* implement
    // filtering logic here */ });
  }
}
