package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  private Long id; // Local SQLite ID
  private Long serverId; // PostgreSQL ID (null for guests)
  private String username; // Null for guests
  private String email; // Null for guests
  private String firstName; // Null for guests
  private String lastName; // Null for guests
  private String deviceId; // Unique device identifier
  private boolean isGuest; // TRUE = guest, FALSE = registered
  private LocalDateTime lastSyncAt;
  private LocalDateTime createdAt;

  // Constructors
  /**
   * Create a guest user (offline mode)
   */
  public static User createGuest() {
    User guest = new User();
    long id = 1;
    guest.id = id;
    guest.deviceId = generateDeviceId();
    guest.isGuest = true;
    guest.serverId = null;
    guest.username = null;
    guest.email = null;
    guest.createdAt = LocalDateTime.now();
    return guest;
  }

  /**
   * Create a registered user
   */
  public static User createRegistered(String username, String email) {
    User user = new User();
    long id = 1;
    user.id = id; // This will be updated when saved to DB
    user.deviceId = generateDeviceId();
    user.isGuest = false;
    user.username = username;
    user.email = email;
    user.createdAt = LocalDateTime.now();
    return user;
  }

  /**
   * Get display name (username or "Guest")
   */
  public String getDisplayName() {
    if (isGuest) {
      return "Guest";
    }
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    }
    return username != null ? username : "User";
  }

  /**
   * Check if user can sync to cloud
   */
  public boolean canSync() {
    return !isGuest && serverId != null;
  }

  /**
   * Convert guest to registered user
   */
  public void promoteToRegistered(String username, String email, Long serverId) {
    this.username = username;
    this.email = email;
    this.serverId = serverId;
    this.isGuest = false;
  }

  /**
   * Generate unique device ID
   */
  private static String generateDeviceId() {
    // Use combination of hostname + random UUID
    String hostname = getHostname();
    String uuid = UUID.randomUUID().toString();
    return hostname + "-" + uuid.substring(0, 8);
  }

  private static String getHostname() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return "unknown";
    }
  }
}
