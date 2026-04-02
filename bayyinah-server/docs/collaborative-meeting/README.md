# Collaborative Meeting Design

This folder documents the end-to-end architecture and runtime flow for the collaborative meeting feature, including STOMP messaging and WebRTC signaling.

## Document map

1. [System Architecture](01-system-architecture.md)
2. [End-to-End Runtime Sequence](02-runtime-sequence.md)
3. [Room State Machine](03-room-state-machine.md)
4. [Validation and Security Gates](04-validation-and-security-gates.md)
5. [WebRTC Signaling Contract](05-webrtc-signaling-contract.md)
6. [JavaFX WebRTC and STOMP Client Research](06-javafx-webrtc-stomp-research.md)

## Main implementation anchors

- Room REST lifecycle endpoints: [HalaqahRestController](../../src/main/java/com/ks/bayyinah/bayyinah_server/controller/HalaqahRestController.java)
- STOMP meeting handlers: [HalaqahController](../../src/main/java/com/ks/bayyinah/bayyinah_server/controller/HalaqahController.java)
- STOMP authentication interceptor: [WebSocketAuthInterceptor](../../src/main/java/com/ks/bayyinah/bayyinah_server/middleware/WebSocketAuthInterceptor.java)
- STOMP broker and endpoint wiring: [WebSocketBrokerConfiguration](../../src/main/java/com/ks/bayyinah/bayyinah_server/config/WebSocketBrokerConfiguration.java)
- Room domain orchestration: [RoomService](../../src/main/java/com/ks/bayyinah/bayyinah_server/service/RoomService.java)
- In-memory room storage: [RoomStorageService](../../src/main/java/com/ks/bayyinah/bayyinah_server/service/RoomStorageService.java)
