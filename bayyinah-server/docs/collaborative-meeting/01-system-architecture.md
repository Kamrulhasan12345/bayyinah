# System Architecture

The feature is split into two planes:

- Lifecycle plane: room create, join, leave, and fetch via REST
- Realtime plane: presence, chat, control, and WebRTC signaling via STOMP

```mermaid
flowchart TB
  classDef client fill:#e8f4ff,stroke:#1e5b8f,color:#0f2e49,stroke-width:1px
  classDef edge fill:#f7f7f7,stroke:#666,color:#222
  classDef server fill:#eef8ef,stroke:#2f7d32,color:#123b15,stroke-width:1px
  classDef store fill:#f3f0ff,stroke:#5f3dc4,color:#2d1b69,stroke-width:1px

  subgraph Clients
    A[Leader Client]
    B[Participant Client]
    C[Other Participants]
  end
  class A,B,C client

  subgraph Entry
    REST[Room REST API]
    WS[WebSocket STOMP Endpoint]
    INT[STOMP Auth Interceptor]
  end
  class REST,WS,INT edge

  subgraph Domain
    HREST[HalaqahRestController]
    HSTOMP[HalaqahController]
    RSVC[RoomService]
    BROKER[Simple Broker topic and queue]
  end
  class HREST,HSTOMP,RSVC,BROKER server

  subgraph Storage
    STORE[RoomStorageService in memory]
  end
  class STORE store

  A -->|Create Join Leave| REST
  B -->|Create Join Leave| REST
  C -->|Create Join Leave| REST

  REST --> HREST --> RSVC --> STORE

  A -->|CONNECT with JWT| WS --> INT
  B -->|CONNECT with JWT| WS --> INT
  C -->|CONNECT with JWT| WS --> INT

  INT -->|Authenticated Principal| HSTOMP
  HSTOMP -->|Presence Signal Chat Control| RSVC
  HSTOMP -->|Fanout events| BROKER
  BROKER --> A
  BROKER --> B
  BROKER --> C
```
