package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
  private String roomId;
  private String senderId;
  private String displayName;
  private String content;
  private String timestamp;
}
