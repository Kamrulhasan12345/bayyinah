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
  private NoteService noteService;
  private MainConfig mainConfig;
  private TokenManager tokenManager;
  private ApiClient apiClient;
  private RemoteUserQueryService remoteUserQueryService;
}
