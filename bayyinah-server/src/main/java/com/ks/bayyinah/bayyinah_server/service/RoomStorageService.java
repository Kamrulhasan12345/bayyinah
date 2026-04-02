package com.ks.bayyinah.bayyinah_server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.dto.stomp.Room;

@Service
public class RoomStorageService {

  private static final Logger logger = LoggerFactory.getLogger(RoomStorageService.class);

  private final Map<String, Room> rooms = new ConcurrentHashMap<>();

  private final Random random = new Random();

  /*
   * Store rooms in memory. In a production environment, you would want to use a
   * more robust storage solution like Redis or a database.
   */
  public Room save(Room room) {
    rooms.put(room.getCode(), room);
    logger.info("Saved room with code={}, leader={}, participants={}", room.getCode(), room.getLeaderId(),
        room.getParticipants().keySet());
    return room;
  }

  /*
   * Find room by code. Returns null if not found.
   */
  public Optional<Room> findByCode(String code) {
    return Optional.ofNullable(rooms.get(code));
  }

  /*
   * Find active room by code. Returns null if not found or if the room is
   * inactive.
   */
  public Optional<Room> findActiveByCode(String code) {
    return findByCode(code).filter(room -> room.getStatus() == Room.RoomStatus.ACTIVE);
  }

  /*
   * Delete room by code.
   */
  public void deleteByCode(String code) {
    Room removed = rooms.remove(code);
    if (removed != null) {
      logger.info("Deleted room with code={}", code);
    }
  }

  /**
   * Check if code exists in storage.
   */
  public boolean codeExists(String code) {
    return rooms.containsKey(code);
  }

  /**
   * Get all active rooms.
   */
  public List<Room> getActiveRooms() {
    return rooms.values().stream().filter(room -> room.getStatus() == Room.RoomStatus.ACTIVE).toList();
  }

  /**
   * Generate a unique 6-character alphanumeric code for a new room.
   */
  public String generateUniqueCode() {
    String code;
    do {
      code = String.format("%06d", random.nextInt(1000000)); // Generate a 6-digit code
    } while (codeExists(code));
    return code;
  }

  /**
   * Cleanup old/closed rooms every hour.
   * Prevents memory leaks by removing rooms that are no longer active.
   * 
   */
  @Scheduled(fixedDelay = 3600000) // Every hour
  public void cleanupOldRooms() {
    LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

    List<String> toRemove = rooms.values().stream()
        .filter(room -> room.getStatus() == Room.RoomStatus.CLOSED ||
            room.getCreatedAt().isBefore(cutoff))
        .map(Room::getCode)
        .toList();

    toRemove.forEach(rooms::remove);

    if (!toRemove.isEmpty()) {
      logger.info("Cleaned up {} old rooms", toRemove.size());
    }
  }

  /**
   * Get statistics about the current rooms in storage.
   */
  public Map<String, Object> getStats() {
    long activeCount = rooms.values().stream().filter(room -> room.getStatus() == Room.RoomStatus.ACTIVE).count();

    long totalParticipants = rooms.values().stream()
        .filter(room -> room.getStatus() == Room.RoomStatus.ACTIVE)
        .mapToLong(room -> room.getParticipants().size())
        .sum();

    return Map.of(
        "totalRooms", rooms.size(),
        "activeRooms", activeCount,
        "totalParticipants", totalParticipants);
  }
}
