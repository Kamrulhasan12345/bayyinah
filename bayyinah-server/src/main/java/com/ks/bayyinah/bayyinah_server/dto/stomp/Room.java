package com.ks.bayyinah.bayyinah_server.dto.stomp;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory room model (no database persistence)
 */
@Data
@Builder
public class Room {

  private String code; // 6-digit code
  private String leaderId; // User ID of leader
  private String leaderName;
  private RoomStatus status;
  private LocalDateTime createdAt;
  private Integer maxParticipants;

  // Current verse being read
  private Integer currentSurah;
  private Integer currentAyah;

  // Thread-safe participant tracking
  private Map<String, Participant> participants; // Use ConcurrentHashMap<>()

  public enum RoomStatus {
    ACTIVE,
    CLOSED
  }

  /**
   * Initialize with thread-safe collections
   */
  public static Room create(String code, Participant leader, Integer maxParticipants) {
    Room room = Room.builder()
        .code(code)
        .leaderId(leader.getId())
        .leaderName(leader.getDisplayName())
        .status(RoomStatus.ACTIVE)
        .createdAt(LocalDateTime.now())
        .maxParticipants(maxParticipants != null ? maxParticipants : 10)
        .participants(new ConcurrentHashMap<>()) // Thread-safe set
        .build();

    // Leader is automatically a participant
    room.participants.put(leader.getId(), leader);

    return room;
  }

  /**
   * Check if user is the leader
   */
  public boolean isLeader(String userId) {
    return leaderId.equals(userId);
  }

  /**
   * Check if user is a participant
   */
  public boolean isParticipant(String userId) {
    return participants.containsKey(userId) || isLeader(userId);
  }

  /**
   * Check if room is full
   */
  public boolean isFull() {
    return participants.size() >= maxParticipants;
  }

  /**
   * Add participant (thread-safe)
   */
  public boolean addParticipant(Participant participant) {
    if (isFull() || participants.containsKey(participant.getId())) {
      return false;
    }
    return participants.putIfAbsent(participant.getId(), participant) == null;
  }

  /**
   * Remove participant (thread-safe)
   */
  public void removeParticipant(String userId) {
    participants.remove(userId);
  }

  /**
   * Get participant count
   */
  public int getParticipantCount() {
    return participants.size();
  }
}
