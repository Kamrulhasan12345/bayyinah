package com.ks.bayyinah.bayyinah_server.dto;

import com.ks.bayyinah.bayyinah_server.dto.stomp.Participant;
import com.ks.bayyinah.bayyinah_server.dto.stomp.Room;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

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
    return RoomResponse.builder()
        .code(room.getCode())
        .leaderId(room.getLeaderId())
        .leaderName(room.getLeaderName())
        .status(room.getStatus().name())
        .createdAt(room.getCreatedAt())
        .maxParticipants(room.getMaxParticipants())
        .participantCount(room.getParticipants().size())
        .participants(room.getParticipants())
        .currentSurah(room.getCurrentSurah())
        .currentAyah(room.getCurrentAyah())
        .build();
  }
}
