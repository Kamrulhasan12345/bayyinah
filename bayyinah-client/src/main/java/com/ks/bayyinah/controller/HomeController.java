package com.ks.bayyinah.controller;

import com.ks.bayyinah.App;
import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.local.query.LocalQuranQueryService;
import com.ks.bayyinah.infra.local.query.LocalQuranSearchQueryService;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import lombok.Setter;

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

  @Setter
  private AppContext appContext;

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
    this.appContext = browsingController.getAppContext();
  }

  public void initializeHome() {
    System.out.println("HomeController initialized with AppContext: " + appContext);
    searchFieldHero.setOnKeyPressed(event -> {
      switch (event.getCode()) {
        case ENTER:
          onSearchEnter();
          break;
        default:
          break;
      }
    });
  }

  private void onSearchEnter() {
    String query = searchFieldHero.getText().trim();

    if (query.isEmpty()) {
      return;
    }

    navigateToSearchResults(query);
  }

  private void navigateToSearchResults(String query) {
    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/SearchResults.fxml"));
      Node root = loader.load();

      LocalQuranSearchQueryService searchService = LocalQuranSearchQueryService.getInstance();
      LocalQuranQueryService quranQueryService = LocalQuranQueryService.getInstance();

      SearchResultsController controller = loader.getController();

      controller.setBrowsingController(browsingController);
      controller.setQuranQueryService(quranQueryService);
      controller.setSearchService(searchService);

      controller.initialize(query);

      browsingController.getContentArea().getChildren().setAll(root);
    } catch (Exception e) {
      System.err.println("Failed to load Search Results view: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
