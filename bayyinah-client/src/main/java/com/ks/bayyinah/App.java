package com.ks.bayyinah;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.ks.bayyinah.infra.local.database.*;
import com.ks.bayyinah.infra.local.repository.user.*;
import com.ks.bayyinah.infra.remote.client.ApiClient;
import com.ks.bayyinah.infra.remote.query.RemoteUserQueryService;
import com.ks.bayyinah.infra.hybrid.service.*;
import com.ks.bayyinah.infra.hybrid.model.*;
import com.ks.bayyinah.infra.hybrid.query.*;
import com.ks.bayyinah.infra.hybrid.query.TokenManager;
import com.ks.bayyinah.config.ConfigManager;
import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.controller.RootController;

/**
 * JavaFX App
 */
public class App extends Application {

  private static Scene scene;

  private AppContext appContext;

  private MainConfig mainConfig;

  @Override
  public void start(Stage stage) throws IOException {
    try {
      mainConfig = ConfigManager.getConfig();
      DatabaseManager.initialize(mainConfig);

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
      rootController.setAppContext(appContext);
      rootController.showBrowsingView();
    }
    return root;
  }

  private void initializeUserServices() {
    appContext = new AppContext();

    // Initialize repositories
    var authTokensRepo = new AuthTokensRepository();
    var bookmarksRepo = new BookmarksRepository();
    var userPrefRepo = new UserPreferenceRepository();
    var readingProgressRepo = new ReadingProgressRepository();
    var userRepo = new UserRepository();
    var noteRepo = new NoteRepository();

    // Initialize services
    var authTokensService = new AuthTokensService(authTokensRepo);
    var bookmarkService = new BookmarkService(bookmarksRepo);
    var userPreferenceService = new UserPreferenceService(userPrefRepo);
    var readingProgressService = new ReadingProgressService(readingProgressRepo);
    var userService = new UserService(userRepo);
    var noteService = new NoteService(noteRepo);

    appContext.setAuthTokensService(authTokensService);
    appContext.setBookmarkService(bookmarkService);
    appContext.setUserPreferenceService(userPreferenceService);
    appContext.setReadingProgressService(readingProgressService);
    appContext.setUserService(userService);
    appContext.setNoteService(noteService);

    var tokenManager = new TokenManager(authTokensService);
    var apiClient = new ApiClient(mainConfig, tokenManager);

    appContext.setMainConfig(mainConfig);
    appContext.setTokenManager(tokenManager);
    appContext.setApiClient(apiClient);

    var remoteUserQueryService = new RemoteUserQueryService(apiClient);
    var authSessionQueryService = new AuthSessionQueryService(authTokensService, userService, remoteUserQueryService);

    appContext.setRemoteUserQueryService(remoteUserQueryService);
    appContext.setAuthSessionQueryService(authSessionQueryService);
  }

  public static void main(String[] args) {
    launch();
  }

}
