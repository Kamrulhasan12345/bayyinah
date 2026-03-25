package com.ks.bayyinah.context;

import com.ks.bayyinah.infra.hybrid.model.MainConfig;
import com.ks.bayyinah.infra.hybrid.query.AuthSessionQueryService;
import com.ks.bayyinah.infra.hybrid.query.TokenManager;
import com.ks.bayyinah.infra.hybrid.service.*;
import com.ks.bayyinah.infra.remote.client.ApiClient;
import com.ks.bayyinah.infra.remote.query.RemoteUserQueryService;
import lombok.Data;

@Data
public class AppContext {
  private AuthSessionQueryService authSessionQueryService;
  private UserService userService;
  private AuthTokensService authTokensService;
  private BookmarkService bookmarkService;
  private UserPreferenceService userPreferenceService;
  private ReadingProgressService readingProgressService;
  private SyncQueueService syncQueueService;
  private NoteService noteService;
  private MainConfig mainConfig;
  private TokenManager tokenManager;
  private ApiClient apiClient;
  private RemoteUserQueryService remoteUserQueryService;

  private AppContext() {
    // Private constructor to prevent instantiation
  }

  private static class Holder {
    private static final AppContext INSTANCE = new AppContext();
  }

  public static AppContext getInstance() {
    return Holder.INSTANCE;
  }
}
