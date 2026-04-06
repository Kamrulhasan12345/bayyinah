# Room State Machine

The room has a compact lifecycle with ACTIVE and CLOSED states.

```mermaid
stateDiagram-v2
  [*] --> ACTIVE: createRoom
  ACTIVE --> ACTIVE: participant join
  ACTIVE --> ACTIVE: participant leave non-leader
  ACTIVE --> CLOSED: leader leave or explicit close
  CLOSED --> [*]
```

## Notes

- Signaling and collaboration messages are accepted only for active rooms.
- Leader departure transitions the room to CLOSED and emits a ROOM_CLOSED control message.
