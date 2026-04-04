package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.hybrid.model.User;
import com.ks.bayyinah.infra.remote.client.StompWebSocketClient;
import com.ks.bayyinah.infra.webrtc.orchestrator.HalaqahSignalingOrchestrator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Meeting foundation controller that binds peer-context signaling orchestration.
 */
public class MeetingViewController {

  @FXML
  private Label placeholderSubtitle;

  private AppContext appContext;
  private HalaqahSignalingOrchestrator signalingOrchestrator;

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  public void initializeMeeting() {
    if (appContext == null || placeholderSubtitle == null) {
      return;
    }

    User currentUser = appContext.getAuthSessionQueryService() != null
        ? appContext.getAuthSessionQueryService().getCurrentUser()
        : null;

    String localUserId = currentUser != null && currentUser.getServerId() != null
        ? String.valueOf(currentUser.getServerId())
        : null;

    if (localUserId == null || localUserId.isBlank()) {
      placeholderSubtitle
          .setText("Collaborative meeting requires an authenticated account before realtime signaling starts.");
      return;
    }

    StompWebSocketClient stompWebSocketClient = new StompWebSocketClient(
        appContext.getMainConfig().getMainApiUrl(),
        appContext.getObjectMapper());

    signalingOrchestrator = new HalaqahSignalingOrchestrator(stompWebSocketClient, localUserId);

    if (appContext.getSignalingOrchestrator() != null) {
      appContext.setSignalingOrchestrator(signalingOrchestrator);
    }

    if (appContext.getStompWebSocketClient() != null) {
      appContext.setStompWebSocketClient(stompWebSocketClient);
    }

    placeholderSubtitle.setText(
        "Meeting signaling foundation initialized. Next step: room create/join + STOMP connect + subscribe.");
  }

  public HalaqahSignalingOrchestrator getSignalingOrchestrator() {
    return signalingOrchestrator;
  }
}
