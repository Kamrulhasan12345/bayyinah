package com.ks.bayyinah.infra.remote.client;

import com.ks.bayyinah.infra.remote.dto.stomp.Candidate;
import com.ks.bayyinah.infra.remote.dto.stomp.ChatMessage;
import com.ks.bayyinah.infra.remote.dto.stomp.Message;
import com.ks.bayyinah.infra.remote.dto.stomp.Presence;
import com.ks.bayyinah.infra.remote.dto.stomp.SdpMessage;
import tools.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
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
import java.util.function.Predicate;

/**
 * STOMP WebSocket client for collaborative sessions
 */
@Data
public class StompWebSocketClient {

  private static final Logger logger = LoggerFactory.getLogger(StompWebSocketClient.class);

  private final String serverUrl;
  private final ObjectMapper objectMapper;

  private WebSocketStompClient stompClient;
  private StompSession session;
  private String roomId;
  private String localUserId;

  public StompWebSocketClient(String serverUrl, ObjectMapper objectMapper) {
    this.serverUrl = serverUrl;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
  }

  /**
   * Connect to WebSocket with JWT authentication
   */
  public CompletableFuture<Void> connect(String roomId, String jwtToken, StompSessionHandler sessionHandler) {
    return connect(roomId, jwtToken, null, sessionHandler);
  }

  /**
   * Connect to WebSocket with JWT authentication and local user context.
   */
  public CompletableFuture<Void> connect(String roomId, String jwtToken, String localUserId,
      StompSessionHandler sessionHandler) {
    this.roomId = roomId;
    this.localUserId = localUserId;

    // Create WebSocket client
    List<Transport> transports = new ArrayList<>();
    try {
      transports.add(new WebSocketTransport(new StandardWebSocketClient()));
    } catch (Throwable throwable) {
      String rootMessage = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
      IllegalStateException wrapped = new IllegalStateException(
          "WebSocket runtime is unavailable. Ensure Jakarta websocket API and Tyrus client runtime are on the classpath. Root cause: "
              + rootMessage,
          throwable);
      logger.error("WebSocket transport initialization failed", wrapped);
      return CompletableFuture.failedFuture(wrapped);
    }

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
  public void subscribeToControl(Consumer<Message> controlHandler) {
    session.subscribe("/topic/room/" + roomId + "/control", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return Message.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Platform.runLater(() -> controlHandler.accept((Message) payload));
      }
    });
  }

  /**
   * Subscribe to SDP offers (WebRTC signaling)
   */
  public void subscribeToOffers(Consumer<SdpMessage> offerHandler) {
    subscribeToOffers(null, offerHandler);
  }

  /**
   * Subscribe to SDP offers with additional peer-context filtering.
   */
  public void subscribeToOffers(Predicate<SdpMessage> filter, Consumer<SdpMessage> offerHandler) {
    session.subscribe("/topic/room/" + roomId + "/offer", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return SdpMessage.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        SdpMessage message = (SdpMessage) payload;
        if (!shouldProcessInboundSignal(message.getSenderId(), filter, message)) {
          return;
        }
        Platform.runLater(() -> offerHandler.accept(message));
      }
    });
  }

  /**
   * Subscribe to SDP answers (WebRTC signaling)
   */
  public void subscribeToAnswers(Consumer<SdpMessage> answerHandler) {
    subscribeToAnswers(null, answerHandler);
  }

  /**
   * Subscribe to SDP answers with additional peer-context filtering.
   */
  public void subscribeToAnswers(Predicate<SdpMessage> filter, Consumer<SdpMessage> answerHandler) {
    session.subscribe("/topic/room/" + roomId + "/answer", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return SdpMessage.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        SdpMessage message = (SdpMessage) payload;
        if (!shouldProcessInboundSignal(message.getSenderId(), filter, message)) {
          return;
        }
        Platform.runLater(() -> answerHandler.accept(message));
      }
    });
  }

  /**
   * Subscribe to ICE candidates (WebRTC signaling)
   */
  public void subscribeToCandidates(Consumer<Candidate> candidateHandler) {
    subscribeToCandidates(null, candidateHandler);
  }

  /**
   * Subscribe to ICE candidates with additional peer-context filtering.
   */
  public void subscribeToCandidates(Predicate<Candidate> filter, Consumer<Candidate> candidateHandler) {
    session.subscribe("/topic/room/" + roomId + "/candidate", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return Candidate.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        Candidate message = (Candidate) payload;
        if (!shouldProcessInboundSignal(message.getSenderId(), filter, message)) {
          return;
        }
        Platform.runLater(() -> candidateHandler.accept(message));
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
  public void sendPresence(String senderId, String displayName, Presence.PresenceType type) {
    Presence presence = new Presence(roomId, senderId, type, displayName);
    session.send("/app/room/" + roomId + "/presence", presence);
  }

  /**
   * Send control message
   */
  public void sendControlMessage(String senderId, Message.MessageType type, String content) {
    Message message = new Message(
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
  public void sendOffer(String senderId, String sdp, String targetUserId, String sessionId) {
    SdpMessage offer = new SdpMessage(SdpMessage.SdpType.OFFER, senderId, roomId, sdp, targetUserId,
        sessionId);
    session.send("/app/room/" + roomId + "/offer", offer);
  }

  /**
   * Send SDP answer
   */
  public void sendAnswer(String senderId, String sdp, String targetUserId, String sessionId) {
    SdpMessage answer = new SdpMessage(SdpMessage.SdpType.ANSWER, senderId, roomId, sdp, targetUserId,
        sessionId);
    session.send("/app/room/" + roomId + "/answer", answer);
  }

  /**
   * Send ICE candidate
   */
  public void sendIceCandidate(String senderId, String candidate, String sdpMid, int sdpMLineIndex,
      String targetUserId, String sessionId) {
    Candidate ice = new Candidate(senderId, roomId, candidate, sdpMid, sdpMLineIndex, targetUserId,
        sessionId);
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

  private <T> boolean shouldProcessInboundSignal(String senderId, Predicate<T> filter, T payload) {
    if (senderId != null && localUserId != null && localUserId.equals(senderId)) {
      logger.debug("Ignoring self-originated signaling frame for user {}", localUserId);
      return false;
    }

    if (filter != null && !filter.test(payload)) {
      logger.debug("Ignoring signaling frame rejected by peer-context filter");
      return false;
    }

    return true;
  }

  public record ErrorMessage(String type, String message) {
  }
}
