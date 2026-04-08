package com.ks.bayyinah.infra.media;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class VerseAudioPlaybackManager {

  public enum PlaybackState {
    IDLE,
    PLAYING,
    PAUSED
  }

  private MediaPlayer activePlayer;
  private String activeVerseKey;
  private PlaybackState playbackState = PlaybackState.IDLE;

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
      this.playbackState = PlaybackState.PLAYING;
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

  public synchronized boolean pause() {
    if (activePlayer == null || playbackState != PlaybackState.PLAYING) {
      return false;
    }
    try {
      activePlayer.pause();
      playbackState = PlaybackState.PAUSED;
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  public synchronized boolean resume() {
    if (activePlayer == null || playbackState != PlaybackState.PAUSED) {
      return false;
    }
    try {
      activePlayer.play();
      playbackState = PlaybackState.PLAYING;
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  public synchronized boolean isPlayingVerse(String verseKey) {
    return activePlayer != null
        && (playbackState == PlaybackState.PLAYING || playbackState == PlaybackState.PAUSED)
        && Objects.equals(activeVerseKey, verseKey);
  }

  public synchronized PlaybackState getPlaybackState() {
    return playbackState;
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
    playbackState = PlaybackState.IDLE;
  }
}
