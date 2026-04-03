package com.ks.bayyinah.infra.remote.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
  private MessageType type;
  private String roomId;
  private String senderId;
  private String content; // JSON payload
  private String timestamp;

  public enum MessageType {
    MUTE,
    UNMUTE,
    VERSE_NAVIGATION, // Leader moved to different verse
    KICK,
    ROOM_CLOSED
  }
}
