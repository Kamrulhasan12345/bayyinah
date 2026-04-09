# bayyinah-client

JavaFX desktop application for reading, studying, and collaborating around Quran content.

## Highlights

- Offline-first reading experience backed by local SQLite data
- Bookmarks, notes, and reading progress services
- Theme system with light/dark/sepia support
- Verse audio playback with reciter-aware lookup
- Realtime halaqah room UI with STOMP signaling and WebRTC audio orchestration

## Runtime architecture

- App entrypoint and bootstrap: `src/main/java/com/ks/bayyinah/App.java`
- Config loading and file generation: `src/main/java/com/ks/bayyinah/config/ConfigManager.java`
- Global app state and services: `src/main/java/com/ks/bayyinah/context/AppContext.java`
- Meeting realtime controller: `src/main/java/com/ks/bayyinah/controller/MeetingViewController.java`
- Theme runtime manager: `src/main/java/com/ks/bayyinah/ui/theme/ThemeManager.java`

## Configuration

On first run, the app creates:

- `~/.bayyinah/config.yaml`
- default local paths for Quran DB, user DB, and audio directory

Update `config.yaml` so it points to generated data artifacts.

Example:

```yaml
quran:
  databasePath: /absolute/path/to/bayyinah/data/quran.db
  apiUrl: https://bayyinah-nvoz.onrender.com
user:
  databasePath: /absolute/path/to/.bayyinah/user.db
  apiUrl: https://bayyinah-nvoz.onrender.com
audio:
  audioRootPath: /absolute/path/to/bayyinah/data/audio
  activeReciterId: 2
apiUrl: https://bayyinah-nvoz.onrender.com
aiApiUrl: https://kamrulhasan12345-bayyinah-ai.hf.space
```

If `databasePath` or `audioRootPath` are wrong, the app may start but fail to load verses/audio.

## Build and run

From repository root:

```bash
mvn -pl bayyinah-client -am clean install
mvn -pl bayyinah-client javafx:run
```

From module directory:

```bash
mvn clean install
mvn javafx:run
```

## Realtime notes

- Realtime room create/join/leave uses server REST APIs.
- Presence/chat/control/signaling uses STOMP routes.
- WebRTC peer setup is coordinated in meeting controller and signaling orchestrator.

For protocol and lifecycle details, see server design docs:

- [Collaborative meeting docs index](../bayyinah-server/docs/collaborative-meeting/README.md)

## Related docs

- [Repository root guide](../readme.md)
- [Core module guide](../bayyinah-core/readme.md)
- [Server module guide](../bayyinah-server/readme.md)
- [Data directory guide](../data/readme.md)
- [Scripts guide](../scripts/readme.md)
