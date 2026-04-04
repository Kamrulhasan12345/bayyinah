package com.ks.bayyinah.bayyinah_server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import com.ks.bayyinah.bayyinah_server.dto.stomp.*;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.service.RoomService;

@Controller
public class HalaqahController {

  private static final Logger logger = LoggerFactory.getLogger(HalaqahController.class);

  @Autowired
  private RoomService roomService;

  @MessageMapping("/room/{roomId}/presence")
  @SendTo("/topic/room/{roomId}/presence")
  @PreAuthorize("isAuthenticated()")
  public Presence handlePresence(@DestinationVariable String roomId, Presence presence,
      @AuthenticationPrincipal UserDetailsImpl user) {
    validateMessage(roomId, presence.getRoomId(), presence.getSenderId(), user, false);

    presence.setDisplayName(HtmlUtils.htmlEscape(presence.getDisplayName()));

    Room room = roomService.getRoom(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));

    Participant participant = room.getParticipants().get(presence.getSenderId());

    if (presence.getType() == Presence.PresenceType.JOIN && participant == null) {
      Participant joiningParticipant = new Participant(
          presence.getSenderId(),
          presence.getDisplayName(),
          java.time.LocalDateTime.now(),
          false,
          false);
      roomService.joinRoom(roomId, joiningParticipant);
      logger.info("User {} joined room {}", presence.getSenderId(), roomId);
    } else if (presence.getType() == Presence.PresenceType.LEAVE && participant != null) {
      roomService.leaveRoom(roomId, participant);
      logger.info("User {} left room {}", presence.getSenderId(), roomId);
    } else if (presence.getType() == Presence.PresenceType.LEAVE) {
      throw new IllegalArgumentException("User not found in room");
    }

    logger.info("Presence in room {}: {} - {}",
        roomId, presence.getType(), presence.getSenderId());

    return presence;
  }

  @MessageMapping("/room/{roomId}/offer")
  @SendTo("/topic/room/{roomId}/offer")
  @PreAuthorize("isAuthenticated()")
  public SdpMessage handleOffer(@DestinationVariable String roomId, SdpMessage offer,
      @AuthenticationPrincipal UserDetailsImpl user) {
    validateMessage(roomId, offer.getRoomId(), offer.getSenderId(), user, true);
    validateSignalingRoute(roomId, offer.getTargetUserId(), offer.getSessionId());

    offer.setSdp(HtmlUtils.htmlEscape(offer.getSdp()));
    offer.setTargetUserId(HtmlUtils.htmlEscape(offer.getTargetUserId()));
    offer.setSessionId(HtmlUtils.htmlEscape(offer.getSessionId()));

    logger.info("SDP offer in room {} from {}", roomId, offer.getSenderId());

    return offer;
  }

  @MessageMapping("/room/{roomId}/answer")
  @SendTo("/topic/room/{roomId}/answer")
  @PreAuthorize("isAuthenticated()")
  public SdpMessage handleAnswer(@DestinationVariable String roomId, SdpMessage answer,
      @AuthenticationPrincipal UserDetailsImpl user) {
    validateMessage(roomId, answer.getRoomId(), answer.getSenderId(), user, true);
    validateSignalingRoute(roomId, answer.getTargetUserId(), answer.getSessionId());

    answer.setSdp(HtmlUtils.htmlEscape(answer.getSdp()));
    answer.setTargetUserId(HtmlUtils.htmlEscape(answer.getTargetUserId()));
    answer.setSessionId(HtmlUtils.htmlEscape(answer.getSessionId()));

    logger.info("SDP answer in room {} from {}", roomId, answer.getSenderId());

    return answer;
  }

  @MessageMapping("/room/{roomId}/candidate")
  @SendTo("/topic/room/{roomId}/candidate")
  @PreAuthorize("isAuthenticated()")
  public Candidate handleCandidate(@DestinationVariable String roomId, Candidate candidate,
      @AuthenticationPrincipal UserDetailsImpl user) {
    validateMessage(roomId, candidate.getRoomId(), candidate.getSenderId(), user, true);
    validateSignalingRoute(roomId, candidate.getTargetUserId(), candidate.getSessionId());

    candidate.setCandidate(HtmlUtils.htmlEscape(candidate.getCandidate()));
    candidate.setSdpMid(HtmlUtils.htmlEscape(candidate.getSdpMid()));
    candidate.setTargetUserId(HtmlUtils.htmlEscape(candidate.getTargetUserId()));
    candidate.setSessionId(HtmlUtils.htmlEscape(candidate.getSessionId()));

    return candidate;
  }

  @MessageMapping("/room/{roomId}/control")
  @SendTo("/topic/room/{roomId}/control")
  @PreAuthorize("isAuthenticated()")
  public Message handleControlMessage(@DestinationVariable String roomId, Message message,
      @AuthenticationPrincipal UserDetailsImpl user) {
    validateMessage(roomId, message.getRoomId(), message.getSenderId(), user, true);

    if (message.getType() == Message.MessageType.MUTE || message.getType() == Message.MessageType.UNMUTE) {
      roomService.setParticipantMuted(roomId, message.getSenderId(), message.getType() == Message.MessageType.MUTE);
    }

    if (message.getType() == Message.MessageType.VERSE_NAVIGATION) {
      if (!roomService.getRoom(roomId).map(r -> r.isLeader(message.getSenderId())).orElse(false)) {
        throw new SecurityException("Only leader can navigate verses");
      }
    }

    message.setContent(HtmlUtils.htmlEscape(message.getContent()));
    message.setTimestamp(HtmlUtils.htmlEscape(message.getTimestamp()));

    logger.info("Control message in room {}: type={}, from={}",
        roomId, message.getType(), message.getSenderId());

    return message;
  }

  @MessageMapping("/room/{roomId}/chat")
  @SendTo("/topic/room/{roomId}/chat")
  @PreAuthorize("isAuthenticated()")
  public ChatMessage handleChatMessage(@DestinationVariable String roomId, ChatMessage chatMessage,
      @AuthenticationPrincipal UserDetailsImpl user) {
    validateMessage(roomId, chatMessage.getRoomId(), chatMessage.getSenderId(), user, true);

    chatMessage.setDisplayName(HtmlUtils.htmlEscape(chatMessage.getDisplayName()));
    chatMessage.setContent(HtmlUtils.htmlEscape(chatMessage.getContent()));
    chatMessage.setTimestamp(HtmlUtils.htmlEscape(chatMessage.getTimestamp()));

    return chatMessage;
  }

  private void validateMessage(String roomId, String messageRoomId, String senderId, UserDetailsImpl user,
      boolean requireInRoom) {
    if (roomId == null || !roomId.equals(messageRoomId)) {
      throw new IllegalArgumentException("Room ID mismatch");
    }

    if (user == null || user.getUser() == null || user.getUser().getId() == null) {
      throw new SecurityException("Unauthenticated user");
    }

    String userId = user.getUser().getId().toString();

    if (userId == null || senderId == null || userId.isEmpty() || senderId.isEmpty()) {
      throw new IllegalArgumentException("User ID or Sender ID is missing");
    }

    if (!userId.equals(senderId)) {
      throw new SecurityException("Sender ID mismatch");
    }

    if (requireInRoom && !roomService.isUserInRoom(roomId, senderId)) {
      throw new SecurityException("User not in room");
    }
  }

  private void validateSignalingRoute(String roomId, String targetUserId, String sessionId) {
    if (targetUserId == null || targetUserId.isBlank()) {
      throw new IllegalArgumentException("targetUserId is required for signaling messages");
    }

    if (sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("sessionId is required for signaling messages");
    }

    if (!roomService.isUserInRoom(roomId, targetUserId)) {
      throw new SecurityException("Target user not in room");
    }
  }

}