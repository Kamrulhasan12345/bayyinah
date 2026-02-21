package com.ks.bayyinah.controller.cell;

import com.ks.bayyinah.App;
import com.ks.bayyinah.controller.VerseController;
import com.ks.bayyinah.core.dto.VerseView;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;

public class VerseCell extends ListCell<VerseView> {

  private Parent root;
  private VerseController controller;

  @Override
  protected void updateItem(VerseView verse, boolean empty) {
    super.updateItem(verse, empty);
    if (empty || verse == null) {
      setGraphic(null);
    } else {
      if (root == null) {
        try {
          FXMLLoader loader = new FXMLLoader(
              App.class.getResource("fxml/component/VerseCell.fxml"));
          root = loader.load();
          controller = loader.getController();

          Region region = (Region) root;
          region
              .prefWidthProperty()
              .bind(getListView().widthProperty().subtract(20));
          region.setMaxWidth(Double.MAX_VALUE);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      controller.bind(verse);
      setGraphic(root);
    }
  }
}
