package com.ks.bayyinah.bayyinah_server.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import com.ks.bayyinah.bayyinah_server.dto.stomp.*;

@Controller
public class HalaqahController {
  // This controller can be used to handle any halaqah-related endpoints in the
  // future.

  @MessageMapping("/room/{roomId}/presence")
  @SendTo("/topic/room/{roomId}/presence")
  public Presence handlePresence(@DestinationVariable String roomId, Presence presence) {
    if (roomId == null || presence == null || roomId.isEmpty() || presence.getRoomId() == null
        || presence.getRoomId().isEmpty() || roomId.equals(presence.getRoomId()) == false) {
      return new Presence(); // Invalid message, ignore
    }
    return new Presence(roomId, presence.getSenderId(), presence.getType(),
        HtmlUtils.htmlEscape(presence.getDisplayName()));
  }

  @MessageMapping("/room/{roomId}/offer")
  @SendTo("/topic/room/{roomId}/offer")
  public SdpMessage handleOffer(@DestinationVariable String roomId, SdpMessage offer) {
    if (roomId == null || offer == null || roomId.isEmpty() || offer.getRoomId() == null
        || offer.getRoomId().isEmpty() || roomId.equals(offer.getRoomId()) == false) {
      return new SdpMessage(); // Invalid message, ignore
    }
    return new SdpMessage(offer.getType(), offer.getSenderId(), roomId, HtmlUtils.htmlEscape(offer.getSdp()));
  }

  @MessageMapping("/room/{roomId}/answer")
  @SendTo("/topic/room/{roomId}/answer")
  public SdpMessage handleAnswer(@DestinationVariable String roomId, SdpMessage answer) {
    if (roomId == null || answer == null || roomId.isEmpty() || answer.getRoomId() == null
        || answer.getRoomId().isEmpty() || roomId.equals(answer.getRoomId()) == false) {
      return new SdpMessage(); // Invalid message, ignore
    }
    return new SdpMessage(answer.getType(), answer.getSenderId(), roomId, HtmlUtils.htmlEscape(answer.getSdp()));
  }

  @MessageMapping("/room/{roomId}/candidate")
  @SendTo("/topic/room/{roomId}/candidate")
  public Candidate handleCandidate(@DestinationVariable String roomId, Candidate candidate) {
    if (roomId == null || candidate == null || roomId.isEmpty() || candidate.getRoomId() == null
        || candidate.getRoomId().isEmpty() || roomId.equals(candidate.getRoomId()) == false) {
      return new Candidate(); // Invalid message, ignore
    }
    return new Candidate(candidate.getSenderId(), roomId, HtmlUtils.htmlEscape(candidate.getCandidate()),
        HtmlUtils.htmlEscape(candidate.getSdpMid()), candidate.getSdpMLineIndex());
  }

  @MessageMapping("/room/{roomId}/control")
  @SendTo("/topic/room/{roomId}/control")
  public Message handleControlMessage(@DestinationVariable String roomId, Message message) {
    if (roomId == null || message == null || roomId.isEmpty() || message.getRoomId() == null
        || message.getRoomId().isEmpty() || roomId.equals(message.getRoomId()) == false) {
      return new Message(); // Invalid message, ignore
    }
    return new Message(message.getType(), roomId, message.getSenderId(),
        HtmlUtils.htmlEscape(message.getContent()), HtmlUtils.htmlEscape(message.getTimestamp()));
  }

  @MessageMapping("/room/{roomId}/chat")
  @SendTo("/topic/room/{roomId}/chat")
  public ChatMessage handleChatMessage(@DestinationVariable String roomId, ChatMessage chatMessage) {
    if (roomId == null || chatMessage == null || roomId.isEmpty() || chatMessage.getRoomId() == null
        || chatMessage.getRoomId().isEmpty() || roomId.equals(chatMessage.getRoomId()) == false) {
      return new ChatMessage(); // Invalid message, ignore
    }
    return new ChatMessage(roomId, chatMessage.getSenderId(),
        HtmlUtils.htmlEscape(chatMessage.getDisplayName()),
        HtmlUtils.htmlEscape(chatMessage.getContent()), HtmlUtils.htmlEscape(chatMessage.getTimestamp()));
  }
}
