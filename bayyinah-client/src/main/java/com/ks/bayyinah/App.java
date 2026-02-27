package com.ks.bayyinah;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.ks.bayyinah.infra.local.database.*;
import com.ks.bayyinah.infra.hybrid.model.MainConfig;
import com.ks.bayyinah.config.ConfigManager;

/**
 * JavaFX App
 */
public class App extends Application {

  private static Scene scene;

  @Override
  public void start(Stage stage) throws IOException {
    try {
      MainConfig config = ConfigManager.getConfig();
      DatabaseManager.initialize(config);
    } catch (Exception e) {
      System.err.println("Failed to initialize database: " + e.getMessage());
      e.printStackTrace();
      return;
    }
    scene = new Scene(loadFXML("fxml/RootLayout"), 1200, 700);
    stage.setScene(scene);
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    DatabaseManager.closeAll();
  }

  static void setRoot(String fxml) throws IOException {
    scene.setRoot(loadFXML(fxml));
  }

  private static Parent loadFXML(String fxml) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
    return fxmlLoader.load();
  }

  public static void main(String[] args) {
    launch();
  }

}
