package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
  private MessageType type;
  private String roomId;
  private String senderId;
  private String content;
  private String timestamp;
}
