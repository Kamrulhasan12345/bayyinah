package com.ks.bayyinah.infra.media;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class VerseAudioPlaybackManager {

  private MediaPlayer activePlayer;
  private String activeVerseKey;

  public synchronized void play(Path audioFile, String verseKey, Runnable onPlaybackEnded, Consumer<String> onError) {
    stop();

    try {
      Media media = new Media(audioFile.toUri().toString());
      MediaPlayer player = new MediaPlayer(media);

      player.setOnEndOfMedia(() -> {
        synchronized (VerseAudioPlaybackManager.this) {
          releaseActivePlayer();
        }
        if (onPlaybackEnded != null) {
          onPlaybackEnded.run();
        }
      });

      player.setOnError(() -> {
        String errorMessage = "Failed to play verse audio.";
        if (player.getError() != null && player.getError().getMessage() != null) {
          errorMessage = player.getError().getMessage();
        }
        synchronized (VerseAudioPlaybackManager.this) {
          releaseActivePlayer();
        }
        if (onError != null) {
          onError.accept(errorMessage);
        }
      });

      this.activePlayer = player;
      this.activeVerseKey = verseKey;
      player.play();
    } catch (Exception ex) {
      releaseActivePlayer();
      if (onError != null) {
        onError.accept("Unable to start verse audio playback.");
      }
    }
  }

  public synchronized void stop() {
    releaseActivePlayer();
  }

  public synchronized boolean isPlayingVerse(String verseKey) {
    return activePlayer != null && Objects.equals(activeVerseKey, verseKey);
  }

  public synchronized void dispose() {
    releaseActivePlayer();
  }

  private void releaseActivePlayer() {
    if (activePlayer != null) {
      try {
        activePlayer.stop();
      } catch (Exception ignored) {
      }
      try {
        activePlayer.dispose();
      } catch (Exception ignored) {
      }
    }

    activePlayer = null;
    activeVerseKey = null;
  }
}
