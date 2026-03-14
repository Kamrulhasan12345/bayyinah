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
import com.ks.bayyinah.error.GlobalExceptionHandler;
import com.ks.bayyinah.ui.ToastManager;

/**
 * JavaFX App
 */
public class App extends Application {

  private static final boolean ENABLE_STARTUP_SAMPLE_TOASTS = false;

  private static Scene scene;

  private AppContext appContext;

  private MainConfig mainConfig;

  private GlobalExceptionHandler exceptionHandler;

  @Override
  public void init() {
    // Set up global exception handler
    exceptionHandler = new GlobalExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

    // Also handle JavaFX thread exceptions
    Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);

  }

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
    stage.setTitle("Bayyinah");

    ToastManager.getInstance().initialize(stage);

    stage.show();

    if (ENABLE_STARTUP_SAMPLE_TOASTS) {
      ToastManager.getInstance().showInfo("Welcome", "Welcome to Bayyinah!");
      ToastManager.getInstance().showInfo("Getting Started", "Use the menu to explore features.");
      ToastManager.getInstance().showSuccess("Setup Complete", "Your application is ready to use.");
      ToastManager.getInstance().showError("Sample Error", "This is a sample error message.");
      ToastManager.getInstance().showWarning("Sample Warning", "This is a sample warning message.");
      ToastManager.getInstance().showDebug("Sample Debug", "This is a sample debug message.");
      ToastManager.getInstance().showInfo("Enjoy!",
          "This is a very very long message. This notification is meant to test how the toast handles long messages. It should wrap properly and still look good without breaking the layout of the application. You can add as much text as you want here to see how it behaves. The toast should expand vertically to accommodate the content while maintaining a reasonable width. And it should still be readable and visually appealing. This is important for providing users with detailed information without overwhelming them. The design should ensure that even with a lot of text, the notification remains user-friendly and effective in communicating the message.");
    }
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
