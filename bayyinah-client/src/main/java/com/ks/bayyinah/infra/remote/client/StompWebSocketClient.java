package com.ks.bayyinah.infra.remote.client;

import tools.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * STOMP WebSocket client for collaborative sessions
 */
public class StompWebSocketClient {

  private static final Logger logger = LoggerFactory.getLogger(StompWebSocketClient.class);

  private final String serverUrl;
  private final ObjectMapper objectMapper;

  private WebSocketStompClient stompClient;
  private StompSession session;
  private String roomId;

  public StompWebSocketClient(String serverUrl, ObjectMapper objectMapper) {
    this.serverUrl = serverUrl;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
  }

  /**
   * Connect to WebSocket with JWT authentication
   */
  public CompletableFuture<Void> connect(String roomId, String jwtToken, StompSessionHandler sessionHandler) {
    this.roomId = roomId;

    // Create WebSocket client
    List<Transport> transports = new ArrayList<>();
    transports.add(new WebSocketTransport(new StandardWebSocketClient()));

    SockJsClient sockJsClient = new SockJsClient(transports);

    stompClient = new WebSocketStompClient(sockJsClient);
    stompClient.setMessageConverter(new JacksonJsonMessageConverter());

    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + jwtToken);

    CompletableFuture<Void> future = new CompletableFuture<>();

    stompClient.connectAsync(serverUrl + "/ws", (WebSocketHttpHeaders) null, connectHeaders, sessionHandler)
        .thenAccept(stompSession -> {
          this.session = stompSession;
          logger.info("WebSocket connected to room: {}", roomId);
          future.complete(null);
        })
        .exceptionally(error -> {
          logger.error("WebSocket connection failed", error);
          future.completeExceptionally(error);
          return null;
        });

    return future;
  }

  /**
   * Subscribe to room chat
   */
  public void subscribeToChat(Consumer<ChatMessage> messageHandler) {
    if (session == null) {
      throw new IllegalStateException("Not connected");
    }

    session.subscribe("/topic/room/" + roomId + "/chat", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ChatMessage.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> messageHandler.accept((ChatMessage) payload));
      }
    });

    logger.info("Subscribed to chat for room: {}", roomId);
  }

  /**
   * Subscribe to presence (join/leave)
   */
  public void subscribeToPresence(Consumer<Presence> presenceHandler) {
    session.subscribe("/topic/room/" + roomId + "/presence", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return Presence.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> presenceHandler.accept((Presence) payload));
      }
    });
  }

  /**
   * Subscribe to control messages (verse navigation, mute, etc.)
   */
  public void subscribeToControl(Consumer<ControlMessage> controlHandler) {
    session.subscribe("/topic/room/" + roomId + "/control", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ControlMessage.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> controlHandler.accept((ControlMessage) payload));
      }
    });
  }

  /**
   * Subscribe to SDP offers (WebRTC signaling)
   */
  public void subscribeToOffers(Consumer<SdpMessage> offerHandler) {
    session.subscribe("/topic/room/" + roomId + "/offer", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return SdpMessage.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> offerHandler.accept((SdpMessage) payload));
      }
    });
  }

  /**
   * Subscribe to SDP answers (WebRTC signaling)
   */
  public void subscribeToAnswers(Consumer<SdpMessage> answerHandler) {
    session.subscribe("/topic/room/" + roomId + "/answer", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return SdpMessage.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> answerHandler.accept((SdpMessage) payload));
      }
    });
  }

  /**
   * Subscribe to ICE candidates (WebRTC signaling)
   */
  public void subscribeToCandidates(Consumer<IceCandidate> candidateHandler) {
    session.subscribe("/topic/room/" + roomId + "/candidate", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return IceCandidate.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> candidateHandler.accept((IceCandidate) payload));
      }
    });
  }

  /**
   * Subscribe to errors
   */
  public void subscribeToErrors(Consumer<ErrorMessage> errorHandler) {
    session.subscribe("/user/queue/errors", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ErrorMessage.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> errorHandler.accept((ErrorMessage) payload));
      }
    });
  }

  /**
   * Send chat message
   */
  public void sendChatMessage(String senderId, String displayName, String content) {
    ChatMessage message = new ChatMessage(
        roomId,
        senderId,
        displayName,
        content,
        System.currentTimeMillis() + "");

    session.send("/app/room/" + roomId + "/chat", message);
  }

  /**
   * Send presence (join/leave)
   */
  public void sendPresence(String senderId, String displayName, PresenceType type) {
    Presence presence = new Presence(roomId, senderId, type, displayName);
    session.send("/app/room/" + roomId + "/presence", presence);
  }

  /**
   * Send control message
   */
  public void sendControlMessage(String senderId, ControlMessageType type, String content) {
    ControlMessage message = new ControlMessage(
        type,
        roomId,
        senderId,
        content,
        System.currentTimeMillis() + "");

    session.send("/app/room/" + roomId + "/control", message);
  }

  /**
   * Send SDP offer (leader only)
   */
  public void sendOffer(String senderId, String sdp) {
    SdpMessage offer = new SdpMessage(SdpType.OFFER, senderId, roomId, sdp);
    session.send("/app/room/" + roomId + "/offer", offer);
  }

  /**
   * Send SDP answer
   */
  public void sendAnswer(String senderId, String sdp) {
    SdpMessage answer = new SdpMessage(SdpType.ANSWER, senderId, roomId, sdp);
    session.send("/app/room/" + roomId + "/answer", answer);
  }

  /**
   * Send ICE candidate
   */
  public void sendIceCandidate(String senderId, String candidate, String sdpMid, int sdpMLineIndex) {
    IceCandidate ice = new IceCandidate(senderId, roomId, candidate, sdpMid, sdpMLineIndex);
    session.send("/app/room/" + roomId + "/candidate", ice);
  }

  /**
   * Disconnect
   */
  public void disconnect() {
    if (session != null && session.isConnected()) {
      session.disconnect();
      logger.info("WebSocket disconnected");
    }
  }

  /**
   * Check if connected
   */
  public boolean isConnected() {
    return session != null && session.isConnected();
  }

  // ════════════════════════════════════════════════════════════
  // MESSAGE DTOs
  // ════════════════════════════════════════════════════════════

  public record ChatMessage(
      String roomId,
      String senderId,
      String displayName,
      String content,
      String timestamp) {
  }

  public record Presence(
      String roomId,
      String senderId,
      PresenceType type,
      String displayName) {
  }

  public enum PresenceType {
    JOIN, LEAVE
  }

  public record ControlMessage(
      ControlMessageType type,
      String roomId,
      String senderId,
      String content,
      String timestamp) {
  }

  public enum ControlMessageType {
    MUTE, UNMUTE, VERSE_NAVIGATION, KICK, ROOM_CLOSED
  }

  public record SdpMessage(
      SdpType type,
      String senderId,
      String roomId,
      String sdp) {
  }

  public enum SdpType {
    OFFER, ANSWER
  }

  public record IceCandidate(
      String senderId,
      String roomId,
      String candidate,
      String sdpMid,
      Integer sdpMLineIndex) {
  }

  public record ErrorMessage(String type, String message) {
  }
}
