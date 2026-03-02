package com.ks.bayyinah;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.ks.bayyinah.infra.local.database.*;
import com.ks.bayyinah.infra.local.repository.user.*;
import com.ks.bayyinah.infra.remote.query.RemoteUserQueryService;
import com.ks.bayyinah.infra.hybrid.service.*;
import com.ks.bayyinah.infra.hybrid.model.*;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.config.ConfigManager;
import com.ks.bayyinah.controller.RootController;

/**
 * JavaFX App
 */
public class App extends Application {

  private static Scene scene;

  private AuthSessionQueryService authSessionQueryService;

  private UserService userService;
  private AuthTokensService authTokensService;
  private BookmarkService bookmarkService;
  private UserPreferenceService userPreferenceService;
  private ReadingProgressService readingProgressService;
  private NoteService noteService;

  private RemoteUserQueryService remoteUserQueryService;

  @Override
  public void start(Stage stage) throws IOException {
    try {
      MainConfig config = ConfigManager.getConfig();
      DatabaseManager.initialize(config);

      this.initializeUserServices();
    } catch (Exception e) {
      System.err.println("Failed to initialize database: " + e.getMessage());
      e.printStackTrace();
      return;
    }
    scene = new Scene(loadRootFXML("fxml/RootLayout"), 1200, 700);
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

  private Parent loadRootFXML(String fxml) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
    Parent root = fxmlLoader.load();
    Object controller = fxmlLoader.getController();

    if (controller instanceof RootController) {
      RootController rootController = (RootController) controller;

      rootController.setAuthTokensService(authTokensService);
      rootController.setBookmarkService(bookmarkService);
      rootController.setUserPreferenceService(userPreferenceService);
      rootController.setReadingProgressService(readingProgressService);
      rootController.setUserService(userService);
      rootController.setNoteService(noteService);

      rootController.setRemoteUserQueryService(remoteUserQueryService);
      rootController.setAuthSessionQueryService(authSessionQueryService);
    }
    return root;
  }

  private void initializeUserServices() {
    // Initialize repositories
    var authTokensRepo = new AuthTokensRepository();
    var bookmarksRepo = new BookmarksRepository();
    var userPrefRepo = new UserPreferenceRepository();
    var readingProgressRepo = new ReadingProgressRepository();
    var userRepo = new UserRepository();
    var noteRepo = new NoteRepository();

    // Initialize services
    authTokensService = new AuthTokensService(authTokensRepo);
    bookmarkService = new BookmarkService(bookmarksRepo);
    userPreferenceService = new UserPreferenceService(userPrefRepo);
    readingProgressService = new ReadingProgressService(readingProgressRepo);
    userService = new UserService(userRepo);
    noteService = new NoteService(noteRepo);

    remoteUserQueryService = new RemoteUserQueryService();

    authSessionQueryService = new AuthSessionQueryService(authTokensService, userService, remoteUserQueryService);
  }

  public static void main(String[] args) {
    launch();
  }

}
