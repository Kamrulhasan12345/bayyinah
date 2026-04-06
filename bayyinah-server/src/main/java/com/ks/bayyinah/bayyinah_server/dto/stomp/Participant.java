package com.ks.bayyinah.bayyinah_server.dto.stomp;

import java.time.LocalDateTime;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
  private String id;
  private String displayName;
  private LocalDateTime connectedAt;
  private boolean isLeader;
  private boolean isMuted;
}
