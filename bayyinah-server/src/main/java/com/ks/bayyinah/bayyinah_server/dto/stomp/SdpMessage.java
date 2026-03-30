package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SdpMessage {
  private String type;
  private String senderId;
  private String roomId;
  private String sdp;
}
