package com.ks.bayyinah.bayyinah_server.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
  public Presence handlePresence(@DestinationVariable String roomId, Presence presence,
      Principal principal) {
    String authenticatedUserId = validateMessage(roomId, presence.getRoomId(), presence.getSenderId(), principal,
        false);

    if (presence.getType() == null) {
      throw new IllegalArgumentException("Presence type is required");
    }

    presence.setSenderId(authenticatedUserId);
    presence.setDisplayName(HtmlUtils.htmlEscape(presence.getDisplayName()));

    Room room = roomService.getRoom(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));

    Participant participant = room.getParticipants().get(authenticatedUserId);

    if (presence.getType() == Presence.PresenceType.JOIN && participant == null) {
      Participant joiningParticipant = new Participant(
          authenticatedUserId,
          presence.getDisplayName(),
          java.time.LocalDateTime.now(),
          false,
          false);
      roomService.joinRoom(roomId, joiningParticipant);
      logger.info("User {} joined room {}", authenticatedUserId, roomId);
    } else if (presence.getType() == Presence.PresenceType.LEAVE && participant != null) {
      roomService.leaveRoom(roomId, participant);
      logger.info("User {} left room {}", authenticatedUserId, roomId);
    } else if (presence.getType() == Presence.PresenceType.LEAVE) {
      throw new IllegalArgumentException("User not found in room");
    }

    logger.info("Presence in room {}: {} - {}",
        roomId, presence.getType(), presence.getSenderId());

    return presence;
  }

  @MessageMapping("/room/{roomId}/offer")
  @SendTo("/topic/room/{roomId}/offer")
  public SdpMessage handleOffer(@DestinationVariable String roomId, SdpMessage offer,
      Principal principal) {
    String authenticatedUserId = validateMessage(roomId, offer.getRoomId(), offer.getSenderId(), principal, true);
    validateSignalingRoute(roomId, offer.getTargetUserId(), offer.getSessionId());

    offer.setSenderId(authenticatedUserId);
    offer.setSdp(HtmlUtils.htmlEscape(offer.getSdp()));
    offer.setTargetUserId(HtmlUtils.htmlEscape(offer.getTargetUserId()));
    offer.setSessionId(HtmlUtils.htmlEscape(offer.getSessionId()));

    logger.info("SDP offer in room {} from {}", roomId, offer.getSenderId());

    return offer;
  }

  @MessageMapping("/room/{roomId}/answer")
  @SendTo("/topic/room/{roomId}/answer")
  public SdpMessage handleAnswer(@DestinationVariable String roomId, SdpMessage answer,
      Principal principal) {
    String authenticatedUserId = validateMessage(roomId, answer.getRoomId(), answer.getSenderId(), principal, true);
    validateSignalingRoute(roomId, answer.getTargetUserId(), answer.getSessionId());

    answer.setSenderId(authenticatedUserId);
    answer.setSdp(HtmlUtils.htmlEscape(answer.getSdp()));
    answer.setTargetUserId(HtmlUtils.htmlEscape(answer.getTargetUserId()));
    answer.setSessionId(HtmlUtils.htmlEscape(answer.getSessionId()));

    logger.info("SDP answer in room {} from {}", roomId, answer.getSenderId());

    return answer;
  }

  @MessageMapping("/room/{roomId}/candidate")
  @SendTo("/topic/room/{roomId}/candidate")
  public Candidate handleCandidate(@DestinationVariable String roomId, Candidate candidate,
      Principal principal) {
    String authenticatedUserId = validateMessage(roomId, candidate.getRoomId(), candidate.getSenderId(), principal,
        true);
    validateSignalingRoute(roomId, candidate.getTargetUserId(), candidate.getSessionId());

    candidate.setSenderId(authenticatedUserId);
    candidate.setCandidate(HtmlUtils.htmlEscape(candidate.getCandidate()));
    candidate.setSdpMid(HtmlUtils.htmlEscape(candidate.getSdpMid()));

    candidate.setTargetUserId(HtmlUtils.htmlEscape(candidate.getTargetUserId()));
    candidate.setSessionId(HtmlUtils.htmlEscape(candidate.getSessionId()));

    return candidate;
  }

  @MessageMapping("/room/{roomId}/control")
  @SendTo("/topic/room/{roomId}/control")
  public Message handleControlMessage(@DestinationVariable String roomId, Message message,
      Principal principal) {
    String authenticatedUserId = validateMessage(roomId, message.getRoomId(), message.getSenderId(), principal, true);

    if (message.getType() == null) {
      throw new IllegalArgumentException("Control message type is required");
    }

    message.setSenderId(authenticatedUserId);

    if (message.getType() == Message.MessageType.MUTE || message.getType() == Message.MessageType.UNMUTE) {
      roomService.setParticipantMuted(roomId, authenticatedUserId, message.getType() == Message.MessageType.MUTE);
    }

    if (message.getType() == Message.MessageType.VERSE_NAVIGATION
        || message.getType() == Message.MessageType.ROOM_CLOSED
        || message.getType() == Message.MessageType.KICK) {
      requireLeader(roomId, authenticatedUserId);
    }

    if (message.getType() == Message.MessageType.ROOM_CLOSED) {
      Room room = roomService.getRoom(roomId)
          .orElseThrow(() -> new IllegalArgumentException("Room not found"));
      roomService.closeRoomSilently(room);
      message.setContent("Room has been closed");
    }

    message.setContent(HtmlUtils.htmlEscape(message.getContent()));
    message.setTimestamp(HtmlUtils.htmlEscape(message.getTimestamp()));

    logger.info("Control message in room {}: type={}, from={}",
        roomId, message.getType(), message.getSenderId());

    return message;
  }

  @MessageMapping("/room/{roomId}/chat")
  @SendTo("/topic/room/{roomId}/chat")
  public ChatMessage handleChatMessage(@DestinationVariable String roomId, ChatMessage chatMessage,
      Principal principal) {
    String authenticatedUserId = validateMessage(roomId, chatMessage.getRoomId(), chatMessage.getSenderId(),
        principal, true);

    chatMessage.setSenderId(authenticatedUserId);

    chatMessage.setDisplayName(HtmlUtils.htmlEscape(chatMessage.getDisplayName()));
    chatMessage.setContent(HtmlUtils.htmlEscape(chatMessage.getContent()));
    chatMessage.setTimestamp(HtmlUtils.htmlEscape(chatMessage.getTimestamp()));

    return chatMessage;
        
  }

  private String validateMessage(String roomId, String messageRoomId, String senderId, Principal principal,
      boolean requireInRoom) {
    if (roomId == null || !roomId.equals(messageRoomId)) {
      throw new IllegalArgumentException("Room ID mismatch");
    }

    String authenticatedUserId = extractAuthenticatedUserId(principal);

    if (senderId != null && !senderId.isBlank() && !authenticatedUserId.equals(senderId)) {
      throw new SecurityException("Sender ID mismatch");
    }

    if (requireInRoom && !roomService.isUserInRoom(roomId, authenticatedUserId)) {
      throw new SecurityException("User not in room");
    }

    return authenticatedUserId;
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

  private String extractAuthenticatedUserId(Principal principal) {
    if (!(principal instanceof UsernamePasswordAuthenticationToken authenticationToken)) {
      throw new SecurityException("Unauthenticated user");
    }

    Object principalObject = authenticationToken.getPrincipal();
    if (!(principalObject instanceof UserDetailsImpl userDetails)
        || userDetails.getUser() == null
        || userDetails.getUser().getId() == null) {
      throw new SecurityException("Unauthenticated user");
    }

    return userDetails.getUser().getId().toString();
  }

  private void requireLeader(String roomId, String authenticatedUserId) {
    if (!roomService.getRoom(roomId).map(r -> r.isLeader(authenticatedUserId)).orElse(false)) {
      throw new SecurityException("Only leader can perform this action");
    }
  }

}
