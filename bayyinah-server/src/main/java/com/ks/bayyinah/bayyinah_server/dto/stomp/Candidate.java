package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {
  private String senderId;
  private String roomId;
  private String candidate;
  private String sdpMid;
  private int sdpMLineIndex;
}
