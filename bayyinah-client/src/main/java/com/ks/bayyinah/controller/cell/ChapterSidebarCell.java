package com.ks.bayyinah.controller.cell;

import com.ks.bayyinah.core.dto.ChapterView;
import com.ks.bayyinah.controller.ChapterSidebarController;
import com.ks.bayyinah.App;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;

import java.io.IOException;

public class ChapterSidebarCell extends ListCell<ChapterView> {

  private Parent root;
  private ChapterSidebarController controller;

  @Override
  protected void updateItem(ChapterView chapter, boolean empty) {
    super.updateItem(chapter, empty);
    if (empty || chapter == null) {
      setGraphic(null);
    } else {
      if (root == null) {
        try {
          FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/component/ChapterSidebarCell.fxml"));
          root = loader.load();
          controller = loader.getController();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      controller.bind(chapter);
      setGraphic(root);
    }
  }
}
