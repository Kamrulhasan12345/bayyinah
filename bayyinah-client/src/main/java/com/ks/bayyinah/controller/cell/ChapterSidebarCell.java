package com.ks.bayyinah.controller.cell;

import com.ks.bayyinah.App;
import com.ks.bayyinah.controller.ChapterSidebarController;
import com.ks.bayyinah.core.dto.ChapterView;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;

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
          FXMLLoader loader = new FXMLLoader(
            App.class.getResource("fxml/component/ChapterSidebarCell.fxml")
          );
          root = loader.load();
          controller = loader.getController();

          Region region = (Region) root;
          region
            .prefWidthProperty()
            .bind(getListView().widthProperty().subtract(20));
          region.maxWidth(Double.MAX_VALUE);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      controller.bind(chapter);
      setGraphic(root);
    }
  }
}
