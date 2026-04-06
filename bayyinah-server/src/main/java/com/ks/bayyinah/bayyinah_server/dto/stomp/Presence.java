package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Presence {
  private String roomId;
  private String senderId;
  private PresenceType type; // JOIN or LEAVE
  private String displayName;

  public enum PresenceType {
    JOIN, LEAVE
  }
}
