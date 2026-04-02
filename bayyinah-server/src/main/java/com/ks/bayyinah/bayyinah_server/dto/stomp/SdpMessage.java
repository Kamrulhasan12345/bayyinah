package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SdpMessage {
  private SdpType type; // OFFER or ANSWER
  private String senderId;
  private String roomId;
  private String sdp; // SDP content

  public enum SdpType {
    OFFER, ANSWER
  }
}
