package com.ks.bayyinah.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class HomeController {

  @FXML
  private VBox homeContainer;

  @FXML
  private FlowPane lastReadFP;

  @FXML
  private ImageView logoImg;

  @FXML
  private TextField searchFieldHero;

  private BrowsingController browsingController;

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }
}
