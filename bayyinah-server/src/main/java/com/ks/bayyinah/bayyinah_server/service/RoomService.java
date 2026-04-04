package com.ks.bayyinah.bayyinah_server.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.dto.stomp.Message;
import com.ks.bayyinah.bayyinah_server.dto.stomp.Participant;
import com.ks.bayyinah.bayyinah_server.dto.stomp.Room;

@Service
public class RoomService {

  private static Logger logger = LoggerFactory.getLogger(RoomService.class);

  @Autowired
  private RoomStorageService storage;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  /**
   * Create a new room with the given leader and max participants.
   */
  public Room createRoom(Participant leader, Integer maxParticipants) {
    String code = storage.generateUniqueCode();

    Room room = Room.create(code, leader, maxParticipants);

    storage.save(room);

    logger.info("Created room: code={}, leader={}", code, leader.getId());

    return room;
  }

  /**
   * Join existing room by code. Returns the updated room or null if room not
   * found or full.
   */
  public Room joinRoom(String code, Participant participant) {
    Room room = storage.findActiveByCode(code)
        .orElseThrow(() -> new IllegalArgumentException("Room not found or inactive"));

    if (room.isFull()) {
      throw new IllegalStateException("Room is full");
    }

    if (!room.addParticipant(participant)) {
      throw new IllegalStateException("Failed to add participant to room");
    }

    storage.save(room);

    logger.info("Participant {} joined room {}", participant.getId(), code);

    return room;
  }

  /**
   * Leave room by code. If the participant is the leader, the room will be closed
   */
  public void leaveRoom(String code, Participant participant) {
    Room room = storage.findActiveByCode(code)
        .orElseThrow(() -> new IllegalArgumentException("Room not found or inactive"));

    boolean wasLeader = room.isLeader(participant.getId());

    room.removeParticipant(participant.getId());

    if (wasLeader) {
      closeRoom(room);
    } else {
      storage.save(room);
    }

    logger.info("User {} left room {}", participant.getId(), code);
  }

  /**
   * Close room by code.
   */
  public void closeRoom(Room room) {
    room.setStatus(Room.RoomStatus.CLOSED);
    storage.save(room);

    messagingTemplate.convertAndSend("/topic/room/" + room.getCode() + "/control",
        new Message(Message.MessageType.ROOM_CLOSED, room.getCode(), "system", "Room has been closed",
            Long.toString(System.currentTimeMillis())));

    logger.info("Closed room {}", room.getCode());
  }

  /**
   * Update current verse being studied in the room. Only leader can update.
   */
  public Room updateCurrentVerse(String code, String userId, int surah, int ayah) {
    Room room = storage.findActiveByCode(code)
        .orElseThrow(() -> new IllegalArgumentException("Room not found or inactive"));

    if (!room.isLeader(userId)) {
      throw new IllegalStateException("Only leader can update current verse");
    }

    room.setCurrentSurah(surah);
    room.setCurrentAyah(ayah);

    storage.save(room);

    logger.info("Updated current verse in room {}: surah={}, ayah={}", code, surah, ayah);

    return room;
  }

  /**
   * Get room by code
   */
  public Optional<Room> getRoom(String code) {
    return storage.findByCode(code);
  }

  /**
   * Verify user is in room by code. Returns the room if user is a participant,
   * otherwise null.
   */
  public boolean isUserInRoom(String code, String userId) {
    return storage.findActiveByCode(code).map(room -> room.isParticipant(userId)).orElse(false);
  }

  /**
   * Update participant mute state in an active room.
   */
  public void setParticipantMuted(String code, String userId, boolean muted) {
    Room room = storage.findActiveByCode(code)
        .orElseThrow(() -> new IllegalArgumentException("Room not found or inactive"));

    Participant participant = room.getParticipants().get(userId);
    if (participant == null) {
      throw new IllegalArgumentException("User not found in room");
    }

    participant.setMuted(muted);
    storage.save(room);
    logger.info("Participant {} mute state in room {} set to {}", userId, code, muted);
  }
}
