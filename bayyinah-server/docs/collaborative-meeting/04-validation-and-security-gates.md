# Validation and Security Gates

Incoming STOMP messages pass through identity, integrity, and membership checks.

```mermaid
flowchart TD
  IN[Incoming STOMP frame] --> A{Room id matches destination}
  A -- No --> E1[Reject invalid room id]
  A -- Yes --> B{Authenticated principal present}
  B -- No --> E2[Reject unauthenticated]
  B -- Yes --> C{sender id equals principal user id}
  C -- No --> E3[Reject sender mismatch]
  C -- Yes --> D{Message type}
  D -->|Presence JOIN| J[Allow before membership check]
  D -->|Offer Answer Candidate Chat Control| M{User in ACTIVE room}
  M -- No --> E4[Reject not in active room]
  M -- Yes --> OK[Accept and fanout]
  J --> OK
```

## Guard locations

- CONNECT and principal attach in [WebSocketAuthInterceptor](../../src/main/java/com/ks/bayyinah/bayyinah_server/middleware/WebSocketAuthInterceptor.java)
- Per-message validation in [HalaqahController](../../src/main/java/com/ks/bayyinah/bayyinah_server/controller/HalaqahController.java)
- Active membership check in [RoomService](../../src/main/java/com/ks/bayyinah/bayyinah_server/service/RoomService.java)
