package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Presence {
  private String roomId;
  private String senderId;
  private PresenceType type; // "join" or "leave"
  private String displayName;
}
