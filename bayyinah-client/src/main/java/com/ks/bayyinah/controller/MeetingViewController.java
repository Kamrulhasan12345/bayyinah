package com.ks.bayyinah.controller;

import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.hybrid.model.User;
import com.ks.bayyinah.infra.remote.dto.stomp.Candidate;
import com.ks.bayyinah.infra.remote.dto.stomp.ChatMessage;
import com.ks.bayyinah.infra.remote.dto.stomp.Message;
import com.ks.bayyinah.infra.remote.dto.stomp.Participant;
import com.ks.bayyinah.infra.remote.dto.stomp.Presence;
import com.ks.bayyinah.infra.remote.dto.stomp.RoomResponse;
import com.ks.bayyinah.infra.remote.dto.stomp.SdpMessage;
import com.ks.bayyinah.infra.remote.client.StompWebSocketClient;
import com.ks.bayyinah.infra.webrtc.manager.WebRtcAudioManager;
import com.ks.bayyinah.infra.webrtc.orchestrator.HalaqahSignalingOrchestrator;
import com.ks.bayyinah.ui.ToastManager;
import dev.onvoid.webrtc.RTCIceCandidate;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Meeting screen controller with room entry, realtime lifecycle, and in-room collaboration scaffold.
 */
public class MeetingViewController {

  @FXML
  private VBox entryPane;

  @FXML
  private BorderPane roomPane;

  @FXML
  private Label statusLabel;

  @FXML
  private TextField createDisplayNameField;

  @FXML
  private Spinner<Integer> maxParticipantsSpinner;

  @FXML
  private TextField joinCodeField;

  @FXML
  private TextField joinDisplayNameField;

  @FXML
  private Button createRoomButton;

  @FXML
  private Button joinRoomButton;

  @FXML
  private Label currentVerseLabel;

  @FXML
  private ListView<String> chapterPreviewListView;

  @FXML
  private VBox leaderControlsPane;

  @FXML
  private TextField surahField;

  @FXML
  private TextField ayahField;

  @FXML
  private Button broadcastVerseButton;

  @FXML
  private ListView<String> participantsListView;

  @FXML
  private ListView<String> chatListView;

  @FXML
  private TextField chatInputField;

  @FXML
  private Button sendChatButton;

  @FXML
  private ToggleButton muteToggleButton;

  @FXML
  private Button leaveRoomButton;

  @FXML
  private Label connectionHintLabel;

  @FXML
  private HeaderController meetingHeaderController;

  private AppContext appContext;
  private HalaqahSignalingOrchestrator signalingOrchestrator;
  private StompWebSocketClient stompWebSocketClient;

  private final ObservableList<String> participantItems = FXCollections.observableArrayList();
  private final ObservableList<String> chatItems = FXCollections.observableArrayList();
  private final Map<String, Participant> participantsById = new HashMap<>();
  private final Map<String, WebRtcAudioManager> rtcBySessionId = new HashMap<>();
  private final Map<String, String> sessionByPeerId = new HashMap<>();
  private final Map<String, List<Candidate>> pendingCandidatesBySessionId = new HashMap<>();
  private final Set<String> sessionsStartingOffer = new HashSet<>();

  private String localUserId;
  private String localDisplayName;
  private String currentRoomCode;
  private String leaderId;
  private boolean isLeader;
  private boolean isMuted;

  private int currentSurah = 1;
  private int currentAyah = 1;

  private Timeline timerTimeline;
  private long roomStartEpochSeconds;

  private enum MeetingState {
    ENTRY,
    CONNECTING,
    IN_ROOM
  }

  private MeetingState meetingState = MeetingState.ENTRY;

  @FXML
  private void initialize() {
    participantsListView.setItems(participantItems);
    chatListView.setItems(chatItems);

    maxParticipantsSpinner.setValueFactory(
        new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(2, 50, 8));

    chapterPreviewListView.setItems(FXCollections.observableArrayList(
        "Al-Fatihah", "Al-Baqarah", "Aal-E-Imran", "An-Nisa", "Al-Ma'idah", "Al-An'am", "Al-A'raf"));

    setState(MeetingState.ENTRY);
  }

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  public void initializeMeeting() {
    if (appContext == null) {
      return;
    }

    if (appContext.getRemoteHalaqahQueryService() == null) {
      setErrorStatus("Meeting service is unavailable. Please restart the app.");
      return;
    }

    User currentUser = appContext.getAuthSessionQueryService() != null
        ? appContext.getAuthSessionQueryService().getCurrentUser()
        : null;

    this.localUserId = currentUser != null && currentUser.getServerId() != null
        ? String.valueOf(currentUser.getServerId())
        : null;

    localDisplayName = currentUser != null ? currentUser.getDisplayName() : "Participant";

    createDisplayNameField.setText(localDisplayName);
    joinDisplayNameField.setText(localDisplayName);

    if (this.localUserId == null || this.localUserId.isBlank() || appContext.getTokenManager() == null) {
      setWarningStatus("Sign in with a registered account to create or join realtime halaqah rooms.");
      disableEntryActions(true);
      return;
    }

    disableEntryActions(false);
    setStatus("Ready. Create a room or join with a code.");
  }

  @FXML
  private void onCreateRoom() {
    if (!ensureAuthenticated()) {
      return;
    }

    String displayName = normalizeDisplayName(createDisplayNameField.getText());
    Integer maxParticipants = maxParticipantsSpinner.getValue();

    if (maxParticipants == null) {
      maxParticipants = 8;
    }

    setState(MeetingState.CONNECTING);
    setStatus("Creating room...");

    appContext.getRemoteHalaqahQueryService().createRoom(displayName, maxParticipants)
        .whenComplete((room, error) -> Platform.runLater(() -> {
          if (error != null) {
            setState(MeetingState.ENTRY);
            setErrorStatus("Failed to create room: " + simplifyError(error));
            return;
          }

          bootstrapRoom(room, displayName);
        }));
  }

  @FXML
  private void onJoinRoom() {
    if (!ensureAuthenticated()) {
      return;
    }

    String code = joinCodeField.getText() == null ? "" : joinCodeField.getText().trim().toUpperCase();
    if (code.isBlank()) {
      setWarningStatus("Enter a room code to join.");
      return;
    }

    String displayName = normalizeDisplayName(joinDisplayNameField.getText());

    setState(MeetingState.CONNECTING);
    setStatus("Joining room " + code + "...");

    appContext.getRemoteHalaqahQueryService().joinRoom(code, displayName)
        .whenComplete((room, error) -> Platform.runLater(() -> {
          if (error != null) {
            setState(MeetingState.ENTRY);
            setErrorStatus("Failed to join room: " + simplifyError(error));
            return;
          }

          bootstrapRoom(room, displayName);
        }));
  }

  @FXML
  private void onSendChat() {
    if (!isConnected() || stompWebSocketClient == null) {
      setWarningStatus("Connect to a room before sending messages.");
      return;
    }

    String message = chatInputField.getText() == null ? "" : chatInputField.getText().trim();
    if (message.isBlank()) {
      return;
    }

    stompWebSocketClient.sendChatMessage(localUserId, localDisplayName, message);
    chatInputField.clear();
  }

  @FXML
  private void onToggleMute() {
    if (!isConnected() || stompWebSocketClient == null) {
      muteToggleButton.setSelected(false);
      setWarningStatus("Join a room first to change audio state.");
      return;
    }

    isMuted = muteToggleButton.isSelected();
    muteToggleButton.setText(isMuted ? "Unmute" : "Mute");
    setLocalMuteState(isMuted);
    stompWebSocketClient.sendControlMessage(localUserId,
        isMuted ? Message.MessageType.MUTE : Message.MessageType.UNMUTE,
        isMuted ? "self-muted" : "self-unmuted");
  }

  @FXML
  private void onBroadcastVerse() {
    if (!isConnected() || stompWebSocketClient == null) {
      setWarningStatus("Join a room before syncing verse focus.");
      return;
    }

    if (!isLeader) {
      setWarningStatus("Only leader can sync verse focus.");
      return;
    }

    Integer surah = parsePositiveInt(surahField.getText());
    Integer ayah = parsePositiveInt(ayahField.getText());

    if (surah == null || ayah == null) {
      setWarningStatus("Enter valid Surah and Ayah numbers.");
      return;
    }

    currentSurah = surah;
    currentAyah = ayah;
    updateVerseFocusLabels();
    stompWebSocketClient.sendControlMessage(localUserId, Message.MessageType.VERSE_NAVIGATION,
        surah + ":" + ayah);
  }

  @FXML
  private void onLeaveRoom() {
    leaveRoom(true);
  }

  private void bootstrapRoom(RoomResponse room, String displayName) {
    if (room == null || room.getCode() == null || room.getCode().isBlank()) {
      setState(MeetingState.ENTRY);
      setErrorStatus("Server returned an invalid room response.");
      return;
    }

    this.currentRoomCode = room.getCode();
    this.localDisplayName = displayName;
    this.leaderId = room.getLeaderId();
    this.isLeader = localUserId != null && localUserId.equals(leaderId);
    this.currentSurah = room.getCurrentSurah() != null ? room.getCurrentSurah() : 1;
    this.currentAyah = room.getCurrentAyah() != null ? room.getCurrentAyah() : 1;

    participantsById.clear();
    if (room.getParticipants() != null) {
      participantsById.putAll(room.getParticipants());
    }
    refreshParticipants();

    updateVerseFocusLabels();
    updateHeader(room.getLeaderName());

    connectToRoom();
  }

  private void connectToRoom() {
    setState(MeetingState.CONNECTING);
    setStatus("Connecting to realtime channel...");
    cleanupAllRtcSessions();

    CompletableFuture<String> tokenFuture = appContext.getTokenManager().getAccessToken();

    tokenFuture.thenCompose(token -> {
      if (appContext.getStompWebSocketClient() != null) {
        appContext.getStompWebSocketClient().disconnect();
      }

      stompWebSocketClient = new StompWebSocketClient(
          appContext.getMainConfig().getMainApiUrl(),
          appContext.getObjectMapper());

      signalingOrchestrator = new HalaqahSignalingOrchestrator(stompWebSocketClient, localUserId);
      appContext.setStompWebSocketClient(stompWebSocketClient);
      appContext.setSignalingOrchestrator(signalingOrchestrator);

      return stompWebSocketClient.connect(currentRoomCode, token, localUserId, new MeetingSessionHandler());
    }).thenRun(() -> Platform.runLater(() -> {
      subscribeRealtime();
      stompWebSocketClient.sendPresence(localUserId, localDisplayName, Presence.PresenceType.JOIN);
      setState(MeetingState.IN_ROOM);
      setStatus("Connected to room " + currentRoomCode + ".");
      setConnectionHint("Connected");
      if (meetingHeaderController != null) {
        meetingHeaderController.setConnectionState("Connected");
      }
      startPeerNegotiationBootstrap();
      startTimer();
    })).exceptionally(error -> {
      Platform.runLater(() -> {
        setState(MeetingState.ENTRY);
        setErrorStatus("Realtime connection failed: " + simplifyError(error));
        setConnectionHint("Disconnected");
        if (meetingHeaderController != null) {
          meetingHeaderController.setConnectionState("Disconnected");
        }
      });
      return null;
    });
  }

  private void subscribeRealtime() {
    stompWebSocketClient.subscribeToPresence(this::handlePresence);
    stompWebSocketClient.subscribeToChat(this::handleChat);
    stompWebSocketClient.subscribeToControl(this::handleControl);
    stompWebSocketClient.subscribeToErrors(error -> setErrorStatus("Realtime error: " + error.message()));

    signalingOrchestrator.subscribeToOffers(this::handleIncomingOfferSignal);
    signalingOrchestrator.subscribeToAnswers(this::handleIncomingAnswerSignal);
    signalingOrchestrator.subscribeToCandidates(this::handleIncomingCandidateSignal);
  }

  private void handlePresence(Presence presence) {
    if (presence == null || presence.getSenderId() == null) {
      return;
    }

    if (presence.getType() == Presence.PresenceType.JOIN) {
      Participant participant = participantsById.getOrDefault(presence.getSenderId(), new Participant());
      participant.setId(presence.getSenderId());
      participant.setDisplayName(normalizeDisplayName(presence.getDisplayName()));
      participant.setLeader(leaderId != null && leaderId.equals(presence.getSenderId()));
      participantsById.put(presence.getSenderId(), participant);

      if (isConnected()
          && !presence.getSenderId().equals(localUserId)
          && shouldInitiateOfferToPeer(presence.getSenderId())) {
        initiateOfferToPeer(presence.getSenderId());
      }
    } else if (presence.getType() == Presence.PresenceType.LEAVE) {
      participantsById.remove(presence.getSenderId());
      cleanupPeerSessions(presence.getSenderId());
    }

    refreshParticipants();
  }

  private void handleChat(ChatMessage chatMessage) {
    if (chatMessage == null || chatMessage.getContent() == null) {
      return;
    }

    String displayName = normalizeDisplayName(chatMessage.getDisplayName());
    chatItems.add(displayName + ": " + chatMessage.getContent());
    if (chatItems.size() > 300) {
      chatItems.remove(0);
    }
    chatListView.scrollTo(chatItems.size() - 1);
  }

  private void handleControl(Message control) {
    if (control == null || control.getType() == null) {
      return;
    }

    if (control.getType() == Message.MessageType.ROOM_CLOSED) {
      setWarningStatus("Room was closed by leader.");
      leaveRoom(false);
      return;
    }

    if (control.getType() == Message.MessageType.VERSE_NAVIGATION) {
      Integer[] parsed = parseVerseContent(control.getContent());
      if (parsed != null) {
        currentSurah = parsed[0];
        currentAyah = parsed[1];
        updateVerseFocusLabels();
      }
      return;
    }

    if (control.getType() == Message.MessageType.MUTE || control.getType() == Message.MessageType.UNMUTE) {
      Participant participant = participantsById.get(control.getSenderId());
      if (participant != null) {
        participant.setMuted(control.getType() == Message.MessageType.MUTE);
        refreshParticipants();
      }
    }
  }

  private void refreshParticipants() {
    List<Participant> participants = new ArrayList<>(participantsById.values());
    participants.sort(Comparator.comparing(p -> p.getDisplayName() == null ? "" : p.getDisplayName()));

    participantItems.clear();
    for (Participant participant : participants) {
      StringBuilder line = new StringBuilder(normalizeDisplayName(participant.getDisplayName()));
      if (participant.isLeader()) {
        line.append(" (leader)");
      }
      if (participant.isMuted()) {
        line.append(" [muted]");
      }
      participantItems.add(line.toString());
    }

    if (meetingHeaderController != null) {
      meetingHeaderController.setParticipantCount(participantItems.size());
    }
  }

  private void updateHeader(String leaderName) {
    if (meetingHeaderController == null) {
      return;
    }

    meetingHeaderController.setRoomCode(currentRoomCode);
    meetingHeaderController.setRole(isLeader);
    meetingHeaderController.setLeader(leaderName);
    meetingHeaderController.setParticipantCount(participantItems.size());
    meetingHeaderController.setConnectionState(meetingState == MeetingState.IN_ROOM ? "Connected" : "Offline");
    meetingHeaderController.setVerseFocus(currentSurah, currentAyah);
  }

  private void updateVerseFocusLabels() {
    currentVerseLabel.setText("Current focus: Surah " + currentSurah + ", Ayah " + currentAyah);
    surahField.setText(String.valueOf(currentSurah));
    ayahField.setText(String.valueOf(currentAyah));
    if (meetingHeaderController != null) {
      meetingHeaderController.setVerseFocus(currentSurah, currentAyah);
    }
  }

  private void setState(MeetingState state) {
    meetingState = state;

    boolean isEntry = state == MeetingState.ENTRY || state == MeetingState.CONNECTING;
    entryPane.setVisible(isEntry);
    entryPane.setManaged(isEntry);

    boolean inRoom = state == MeetingState.IN_ROOM;
    roomPane.setVisible(inRoom);
    roomPane.setManaged(inRoom);

    boolean isBusy = state == MeetingState.CONNECTING;
    disableEntryActions(isBusy);

    leaderControlsPane.setDisable(!isLeader || !inRoom);
    broadcastVerseButton.setDisable(!isLeader || !inRoom);
    sendChatButton.setDisable(!inRoom);
    muteToggleButton.setDisable(!inRoom);
    leaveRoomButton.setDisable(!inRoom);
    chatInputField.setDisable(!inRoom);
  }

  private void disableEntryActions(boolean disable) {
    createRoomButton.setDisable(disable);
    joinRoomButton.setDisable(disable);
    maxParticipantsSpinner.setDisable(disable);
    createDisplayNameField.setDisable(disable);
    joinCodeField.setDisable(disable);
    joinDisplayNameField.setDisable(disable);
  }

  private void startTimer() {
    stopTimer();
    roomStartEpochSeconds = System.currentTimeMillis() / 1000;
    timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
      long elapsed = (System.currentTimeMillis() / 1000) - roomStartEpochSeconds;
      long minutes = elapsed / 60;
      long seconds = elapsed % 60;
      String value = String.format("%02d:%02d", minutes, seconds);
      if (meetingHeaderController != null) {
        meetingHeaderController.setTimer(value);
      }
    }));
    timerTimeline.setCycleCount(Timeline.INDEFINITE);
    timerTimeline.play();
  }

  private void stopTimer() {
    if (timerTimeline != null) {
      timerTimeline.stop();
      timerTimeline = null;
    }

    if (meetingHeaderController != null) {
      meetingHeaderController.setTimer("00:00");
    }
  }

  private void leaveRoom(boolean requestLeaveApi) {
    stopTimer();
    cleanupAllRtcSessions();

    if (stompWebSocketClient != null && stompWebSocketClient.isConnected()) {
      try {
        stompWebSocketClient.sendPresence(localUserId, localDisplayName, Presence.PresenceType.LEAVE);
      } catch (Exception ignored) {
        // Best effort leave signal.
      }
      stompWebSocketClient.disconnect();
    }

    if (requestLeaveApi && appContext != null && appContext.getRemoteHalaqahQueryService() != null
        && currentRoomCode != null && !currentRoomCode.isBlank()) {
      appContext.getRemoteHalaqahQueryService().leaveRoom(currentRoomCode)
          .exceptionally(error -> null);
    }

    currentRoomCode = null;
    participantsById.clear();
    participantItems.clear();
    chatItems.clear();
    isMuted = false;
    muteToggleButton.setSelected(false);
    muteToggleButton.setText("Mute");

    setConnectionHint("Disconnected");
    if (meetingHeaderController != null) {
      meetingHeaderController.setConnectionState("Offline");
      meetingHeaderController.setRoomCode("-");
      meetingHeaderController.setParticipantCount(0);
    }

    isLeader = false;
    setState(MeetingState.ENTRY);
    setStatus("You left the room.");
  }

  private boolean isConnected() {
    return meetingState == MeetingState.IN_ROOM && stompWebSocketClient != null && stompWebSocketClient.isConnected();
  }

  private boolean ensureAuthenticated() {
    if (localUserId == null || localUserId.isBlank() || appContext == null || appContext.getTokenManager() == null) {
      setWarningStatus("Sign in with a registered account to continue.");
      return false;
    }
    return true;
  }

  private String normalizeDisplayName(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      return localDisplayName != null && !localDisplayName.isBlank() ? localDisplayName : "Participant";
    }
    return displayName.trim();
  }

  private Integer parsePositiveInt(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : null;
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private Integer[] parseVerseContent(String content) {
    if (content == null || content.isBlank()) {
      return null;
    }

    String[] split = content.trim().split(":");
    if (split.length != 2) {
      return null;
    }

    Integer surah = parsePositiveInt(split[0]);
    Integer ayah = parsePositiveInt(split[1]);
    if (surah == null || ayah == null) {
      return null;
    }

    return new Integer[] { surah, ayah };
  }

  private void startPeerNegotiationBootstrap() {
    if (!isConnected()) {
      return;
    }

    for (Participant participant : participantsById.values()) {
      if (participant == null || participant.getId() == null || participant.getId().equals(localUserId)) {
        continue;
      }
      if (shouldInitiateOfferToPeer(participant.getId())) {
        initiateOfferToPeer(participant.getId());
      }
    }
  }

  private void initiateOfferToPeer(String peerId) {
    if (peerId == null || peerId.isBlank() || peerId.equals(localUserId) || !isConnected()
        || signalingOrchestrator == null || stompWebSocketClient == null) {
      return;
    }

    if (sessionsStartingOffer.contains(peerId)) {
      return;
    }

    String existingSessionId = sessionByPeerId.get(peerId);
    if (existingSessionId != null && rtcBySessionId.containsKey(existingSessionId)) {
      return;
    }

    sessionsStartingOffer.add(peerId);
    String sessionId = UUID.randomUUID().toString();
    sessionByPeerId.put(peerId, sessionId);
    signalingOrchestrator.trackSession(sessionId);

    WebRtcAudioManager manager;
    try {
      manager = createRtcManager(peerId, sessionId, true);
    } catch (Throwable error) {
      sessionsStartingOffer.remove(peerId);
      cleanupSession(sessionId);
      setErrorStatus("Failed to initialize audio pipeline: " + simplifyError(error));
      return;
    }

    rtcBySessionId.put(sessionId, manager);
    attachPendingCandidates(sessionId, manager);

    manager.createOffer()
        .thenAccept(offerSdp -> stompWebSocketClient.sendOffer(localUserId, offerSdp, peerId, sessionId))
        .whenComplete((unused, error) -> Platform.runLater(() -> {
          sessionsStartingOffer.remove(peerId);
          if (error != null) {
            cleanupSession(sessionId);
            setErrorStatus("Failed to create offer for " + peerId + ": " + simplifyError(error));
          }
        }));
  }

  private WebRtcAudioManager createRtcManager(String peerId, String sessionId, boolean localIsLeaderForSession) {
    WebRtcAudioManager manager = new WebRtcAudioManager(localUserId);
    manager.createPeerConnection(localIsLeaderForSession);
    manager.startAudioCapture();
    manager.setMuted(isMuted);
    manager.setOnIceCandidateGenerated(
        candidate -> sendIceCandidateToPeer(peerId, sessionId, candidate));
    return manager;
  }

  private boolean shouldInitiateOfferToPeer(String peerId) {
    if (peerId == null || peerId.isBlank() || localUserId == null || localUserId.isBlank()) {
      return false;
    }

    if (peerId.equals(localUserId)) {
      return false;
    }

    Long localNumeric = tryParseLong(localUserId);
    Long peerNumeric = tryParseLong(peerId);
    if (localNumeric != null && peerNumeric != null) {
      return localNumeric < peerNumeric;
    }

    return localUserId.compareTo(peerId) < 0;
  }

  private Long tryParseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private void sendIceCandidateToPeer(String peerId, String sessionId, RTCIceCandidate candidate) {
    if (candidate == null || !isConnected() || stompWebSocketClient == null) {
      return;
    }

    try {
      stompWebSocketClient.sendIceCandidate(localUserId, candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex,
          peerId, sessionId);
    } catch (Exception error) {
      Platform.runLater(() -> setWarningStatus("Failed to send ICE candidate: " + simplifyError(error)));
    }
  }

  private void handleIncomingOfferSignal(SdpMessage offer) {
    if (offer == null || offer.getSenderId() == null || offer.getSdp() == null || offer.getSdp().isBlank()) {
      return;
    }

    String sessionId = offer.getSessionId();
    if (sessionId == null || sessionId.isBlank()) {
      setWarningStatus("Ignoring offer without session id.");
      return;
    }

    String peerId = offer.getSenderId();
    String previousSessionForPeer = sessionByPeerId.put(peerId, sessionId);
    if (previousSessionForPeer != null && !previousSessionForPeer.equals(sessionId)) {
      cleanupSession(previousSessionForPeer);
    }

    signalingOrchestrator.trackSession(sessionId);

    WebRtcAudioManager manager = rtcBySessionId.get(sessionId);
    if (manager == null) {
      try {
        manager = createRtcManager(peerId, sessionId, false);
      } catch (Throwable error) {
        setErrorStatus("Failed to initialize participant audio pipeline: " + simplifyError(error));
        return;
      }
      rtcBySessionId.put(sessionId, manager);
      attachPendingCandidates(sessionId, manager);
    }

    manager.handleOffer(offer.getSdp())
        .thenAccept(answerSdp -> stompWebSocketClient.sendAnswer(localUserId, answerSdp, peerId, sessionId))
        .exceptionally(error -> {
          Platform.runLater(() -> {
            cleanupSession(sessionId);
            setErrorStatus("Failed to process offer: " + simplifyError(error));
          });
          return null;
        });
  }

  private void handleIncomingAnswerSignal(SdpMessage answer) {
    if (answer == null || answer.getSessionId() == null || answer.getSessionId().isBlank()) {
      return;
    }

    String sessionId = answer.getSessionId();
    WebRtcAudioManager manager = rtcBySessionId.get(sessionId);
    if (manager == null) {
      setWarningStatus("Received answer for unknown session.");
      return;
    }

    if (answer.getSenderId() != null && !answer.getSenderId().isBlank()) {
      sessionByPeerId.put(answer.getSenderId(), sessionId);
    }

    manager.handleAnswer(answer.getSdp()).exceptionally(error -> {
      Platform.runLater(() -> {
        cleanupSession(sessionId);
        setErrorStatus("Failed to apply answer: " + simplifyError(error));
      });
      return null;
    });
  }

  private void handleIncomingCandidateSignal(Candidate candidate) {
    if (candidate == null || candidate.getSessionId() == null || candidate.getSessionId().isBlank()) {
      return;
    }

    String sessionId = candidate.getSessionId();
    WebRtcAudioManager manager = rtcBySessionId.get(sessionId);
    if (manager == null) {
      pendingCandidatesBySessionId.computeIfAbsent(sessionId, key -> new ArrayList<>()).add(candidate);
      return;
    }

    manager.addIceCandidate(candidate.getCandidate(), candidate.getSdpMid(), candidate.getSdpMLineIndex());
  }

  private void attachPendingCandidates(String sessionId, WebRtcAudioManager manager) {
    List<Candidate> pendingCandidates = pendingCandidatesBySessionId.remove(sessionId);
    if (pendingCandidates == null || pendingCandidates.isEmpty()) {
      return;
    }

    for (Candidate candidate : pendingCandidates) {
      manager.addIceCandidate(candidate.getCandidate(), candidate.getSdpMid(), candidate.getSdpMLineIndex());
    }
  }

  private void cleanupPeerSessions(String peerId) {
    if (peerId == null || peerId.isBlank()) {
      return;
    }

    String sessionId = sessionByPeerId.remove(peerId);
    if (sessionId != null) {
      cleanupSession(sessionId);
    }
    sessionsStartingOffer.remove(peerId);
  }

  private void cleanupSession(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }

    WebRtcAudioManager manager = rtcBySessionId.remove(sessionId);
    if (manager != null) {
      disposeManagerQuietly(manager);
    }

    pendingCandidatesBySessionId.remove(sessionId);
    sessionByPeerId.entrySet().removeIf(entry -> sessionId.equals(entry.getValue()));
    if (signalingOrchestrator != null) {
      signalingOrchestrator.untrackSession(sessionId);
    }
  }

  private void cleanupAllRtcSessions() {
    for (WebRtcAudioManager manager : rtcBySessionId.values()) {
      disposeManagerQuietly(manager);
    }
    rtcBySessionId.clear();
    pendingCandidatesBySessionId.clear();
    sessionByPeerId.clear();
    sessionsStartingOffer.clear();
    if (signalingOrchestrator != null) {
      signalingOrchestrator.clearSessions();
    }
  }

  private void disposeManagerQuietly(WebRtcAudioManager manager) {
    try {
      manager.dispose();
    } catch (Throwable error) {
      // Keep UI alive even if native WebRTC objects fail to tear down cleanly.
      setWarningStatus("Media cleanup completed with warnings. You can continue using the app.");
    }
  }

  private void setLocalMuteState(boolean muted) {
    for (WebRtcAudioManager manager : rtcBySessionId.values()) {
      manager.setMuted(muted);
    }
  }

  private String simplifyError(Throwable error) {
    Throwable cause = error;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    String message = cause.getMessage();
    if (message == null || message.isBlank()) {
      message = "Unexpected error";
    }

    String normalized = message.toLowerCase();
    if (normalized.contains("jakarta.websocket.clientendpointconfig$configurator")
        || normalized.contains("jakarta.websocket")
        || normalized.contains("websocket runtime is unavailable")) {
      return "WebSocket runtime dependency issue detected (Jakarta/Tyrus). Rebuild and restart the app after updating dependencies.";
    }

    return message;
  }

  private void setStatus(String value) {
    statusLabel.setText(value);
  }

  private void setWarningStatus(String value) {
    statusLabel.setText(value);
    ToastManager.getInstance().showWarning("Meeting", value);
  }

  private void setErrorStatus(String value) {
    statusLabel.setText(value);
    ToastManager.getInstance().showError("Meeting", value);
  }

  private void setConnectionHint(String value) {
    connectionHintLabel.setText(value);
  }

  private class MeetingSessionHandler extends StompSessionHandlerAdapter {
    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
      // Connection completion is handled by the connect future chain.
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers,
        byte[] payload, Throwable exception) {
      Platform.runLater(() -> setErrorStatus("STOMP error: " + simplifyError(exception)));
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
      Platform.runLater(() -> {
        setErrorStatus("Transport error: " + simplifyError(exception));
        setConnectionHint("Connection interrupted");
        if (meetingHeaderController != null) {
          meetingHeaderController.setConnectionState("Interrupted");
        }
      });
    }
  }

  public HalaqahSignalingOrchestrator getSignalingOrchestrator() {
    return signalingOrchestrator;
  }
}
