package com.ks.bayyinah.infra.remote.dto.stomp;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
  private String code;
  private String leaderId;
  private String leaderName;
  private String status;
  private LocalDateTime createdAt;
  private Integer maxParticipants;
  private Integer participantCount;
  private Map<String, Participant> participants;
  private Integer currentSurah;
  private Integer currentAyah;

  public static RoomResponse from(Room room) {
    Map<String, Participant> participantSnapshot =
        room.getParticipants() == null ? Map.of() : Map.copyOf(room.getParticipants());

    return RoomResponse.builder()
        .code(room.getCode())
        .leaderId(room.getLeaderId())
        .leaderName(room.getLeaderName())
        .status(room.getStatus().name())
        .createdAt(room.getCreatedAt())
        .maxParticipants(room.getMaxParticipants())
        .participantCount(participantSnapshot.size())
        .participants(participantSnapshot)
        .currentSurah(room.getCurrentSurah())
        .currentAyah(room.getCurrentAyah())
        .build();
  }
  }
}
