package com.ks.bayyinah.infra.webrtc.orchestrator;

import com.ks.bayyinah.infra.remote.client.StompWebSocketClient;
import com.ks.bayyinah.infra.remote.dto.stomp.Candidate;
import com.ks.bayyinah.infra.remote.dto.stomp.SdpMessage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Routes broadcast signaling frames through local session-aware filters.
 */
public class HalaqahSignalingOrchestrator {

  private final StompWebSocketClient stompWebSocketClient;
  private final String localUserId;
  private final Set<String> trackedSessionIds = Collections.synchronizedSet(new HashSet<>());

  public HalaqahSignalingOrchestrator(StompWebSocketClient stompWebSocketClient, String localUserId) {
    this.stompWebSocketClient = Objects.requireNonNull(stompWebSocketClient, "stompWebSocketClient is required");
    this.localUserId = localUserId;
    this.stompWebSocketClient.setLocalUserId(localUserId);
  }

  public void trackSession(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    trackedSessionIds.add(sessionId);
  }

  public void untrackSession(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    trackedSessionIds.remove(sessionId);
  }

  public void clearSessions() {
    trackedSessionIds.clear();
  }

  public void subscribeToOffers(Consumer<SdpMessage> offerHandler) {
    stompWebSocketClient.subscribeToOffers(this::allowSignalFromPeerContext, offerHandler);
  }

  public void subscribeToAnswers(Consumer<SdpMessage> answerHandler) {
    stompWebSocketClient.subscribeToAnswers(this::allowSignalFromPeerContext, answerHandler);
  }

  public void subscribeToCandidates(Consumer<Candidate> candidateHandler) {
    stompWebSocketClient.subscribeToCandidates(this::allowSignalFromPeerContext, candidateHandler);
  }

  private boolean allowSignalFromPeerContext(SdpMessage message) {
    return allowSenderAndSession(message.getSenderId(), message.getSessionId(), message.getTargetUserId());
  }

  private boolean allowSignalFromPeerContext(Candidate message) {
    return allowSenderAndSession(message.getSenderId(), message.getSessionId(), message.getTargetUserId());
  }

  private boolean allowSenderAndSession(String senderId, String sessionId, String targetUserId) {
    if (senderId == null || senderId.isBlank()) {
      return false;
    }

    if (localUserId != null && localUserId.equals(senderId)) {
      return false;
    }

    if (targetUserId != null && !targetUserId.isBlank() && localUserId != null && !localUserId.equals(targetUserId)) {
      return false;
    }

    if (trackedSessionIds.isEmpty()) {
      return true;
    }

    if (sessionId != null && trackedSessionIds.contains(sessionId)) {
      return true;
    }

    // Allow explicitly targeted frames so newly-created sessions are not dropped
    // before local tracking is updated.
    return targetUserId != null
        && !targetUserId.isBlank()
        && localUserId != null
        && localUserId.equals(targetUserId);
  }
}