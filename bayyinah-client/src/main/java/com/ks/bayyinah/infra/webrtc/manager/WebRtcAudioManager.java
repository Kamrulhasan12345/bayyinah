package com.ks.bayyinah.infra.webrtc.manager;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.*;
import dev.onvoid.webrtc.media.audio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * WebRTC audio manager using webrtc-java
 * Handles peer connection, audio capture, and playback
 */
public class WebRtcAudioManager {

  private static final Logger logger = LoggerFactory.getLogger(WebRtcAudioManager.class);

  // WebRTC components
  private PeerConnectionFactory factory;
  private RTCPeerConnection peerConnection;
  private AudioDeviceModule audioDeviceModule;
  private RTCRtpSender localAudioSender;
  private AudioTrack localAudioTrack;
  private final List<RTCIceCandidate> pendingRemoteCandidates = new ArrayList<>();
  private volatile boolean remoteDescriptionSet;

  // Callbacks
  private Consumer<String> onSdpCreated;
  private Consumer<RTCIceCandidate> onIceCandidateGenerated;

  private boolean isLeader;
  private String userId;

  public WebRtcAudioManager(String userId) {
    this.userId = userId;
    initializeWebRtc();
  }

  /**
   * Initialize WebRTC factory and audio device
   */
  private void initializeWebRtc() {
    Throwable defaultFactoryFailure = null;

    try {
      // Official quickstart path: default factory handles native audio setup.
      factory = new PeerConnectionFactory();
      logger.info("WebRTC initialized using default PeerConnectionFactory");
      return;
    } catch (Throwable throwable) {
      defaultFactoryFailure = throwable;
      logger.warn("Default WebRTC factory initialization failed, falling back to explicit AudioDeviceModule",
          throwable);
    }

    try {
      // Optional path for explicit audio device selection and custom audio layer.
      audioDeviceModule = new AudioDeviceModule();
      factory = new PeerConnectionFactory(audioDeviceModule);
      logger.info("WebRTC initialized using explicit AudioDeviceModule");
    } catch (Throwable throwable) {
      IllegalStateException wrapped = new IllegalStateException(
          "WebRTC initialization failed. Verify webrtc-java native runtime for this platform, a 64-bit JDK, and required Windows VC++ runtime.",
          throwable);
      if (defaultFactoryFailure != null) {
        wrapped.addSuppressed(defaultFactoryFailure);
      }
      logger.error("Failed to initialize WebRTC", wrapped);
      throw wrapped;
    }
  }

  /**
   * Create peer connection with STUN server
   */
  public void createPeerConnection(boolean isLeader) {
    this.isLeader = isLeader;
    this.remoteDescriptionSet = false;
    this.pendingRemoteCandidates.clear();

    if (factory == null) {
      throw new IllegalStateException("WebRTC factory is not initialized");
    }

    // ICE servers (STUN for NAT traversal)
    RTCConfiguration config = new RTCConfiguration();
    RTCIceServer rtcIceServer = new RTCIceServer();
    rtcIceServer.urls.add("stun:stun.l.google.com:19302");
    config.iceServers.add(rtcIceServer);

    // Create peer connection
    peerConnection = factory.createPeerConnection(config, new PeerConnectionObserver() {

      @Override
      public void onIceCandidate(RTCIceCandidate candidate) {
        logger.info("ICE candidate generated: {}", candidate.sdp);
        if (onIceCandidateGenerated != null) {
          onIceCandidateGenerated.accept(candidate);
        }
      }

      @Override
      public void onTrack(RTCRtpTransceiver transceiver) {
        var receiver = transceiver.getReceiver();
        var track = receiver.getTrack();

        if (track instanceof AudioTrack) {
          logger.info("Handling remote audio track via transceiver");
        }
      }

      @Override
      public void onIceConnectionChange(RTCIceConnectionState state) {
        logger.info("ICE connection state: {}", state);
      }

      @Override
      public void onConnectionChange(RTCPeerConnectionState state) {
        logger.info("Peer connection state: {}", state);
      }
    });

    logger.info("Peer connection created (leader: {})", isLeader);
  }

  /**
   * Start local audio capture and attach track to the current peer connection.
   */
  public void startAudioCapture() {
    if (peerConnection == null) {
      logger.warn("Cannot start audio capture before peer connection is created");
      return;
    }

    if (localAudioTrack != null) {
      localAudioTrack.setEnabled(true);
      logger.info("Audio capture already active");
      return;
    }

    try {
      // Create audio source
      AudioOptions audioOptions = new AudioOptions();
      audioOptions.echoCancellation = true;
      audioOptions.autoGainControl = true;
      audioOptions.noiseSuppression = true;

      AudioTrackSource audioSource = factory.createAudioSource(audioOptions);

      // Create audio track
      localAudioTrack = factory.createAudioTrack("audio-track-" + userId, audioSource);

      // Add track to peer connection
      localAudioSender = peerConnection.addTrack(localAudioTrack, List.of("stream-" + userId));

      logger.info("Audio capture started");

    } catch (Exception e) {
      logger.error("Failed to start audio capture", e);
    }
  }

  /**
   * Stop audio capture
   */
  public void stopAudioCapture() {
    if (localAudioTrack != null) {
      localAudioTrack.setEnabled(false);
      logger.info("Audio capture stopped");
    }
  }

  /**
   * Create SDP offer (leader initiates)
   */
  public CompletableFuture<String> createOffer() {
    CompletableFuture<String> future = new CompletableFuture<>();

    RTCOfferOptions options = new RTCOfferOptions();

    peerConnection.createOffer(options, new CreateSessionDescriptionObserver() {
      @Override
      public void onSuccess(RTCSessionDescription sdp) {
        // Set local description
        peerConnection.setLocalDescription(sdp, new SetSessionDescriptionObserver() {
          @Override
          public void onSuccess() {
            logger.info("SDP offer created and set as local description");
            future.complete(sdp.sdp);
          }

          @Override
          public void onFailure(String error) {
            logger.error("Failed to set local description: {}", error);
            future.completeExceptionally(new RuntimeException(error));
          }
        });
      }

      @Override
      public void onFailure(String error) {
        logger.error("Failed to create offer: {}", error);
        future.completeExceptionally(new RuntimeException(error));
      }
    });

    return future;
  }

  /**
   * Handle received SDP offer (participant receives)
   */
  public CompletableFuture<String> handleOffer(String sdp) {
    CompletableFuture<String> future = new CompletableFuture<>();

    RTCSessionDescription offer = new RTCSessionDescription(RTCSdpType.OFFER, sdp);

    // Set remote description
    peerConnection.setRemoteDescription(offer, new SetSessionDescriptionObserver() {
      @Override
      public void onSuccess() {
        logger.info("Remote offer set, creating answer");
        remoteDescriptionSet = true;
        flushPendingRemoteCandidates();

        // Create answer
        RTCAnswerOptions answerOptions = new RTCAnswerOptions();
        peerConnection.createAnswer(answerOptions, new CreateSessionDescriptionObserver() {
          @Override
          public void onSuccess(RTCSessionDescription answer) {
            // Set local description
            peerConnection.setLocalDescription(answer, new SetSessionDescriptionObserver() {
              @Override
              public void onSuccess() {
                logger.info("SDP answer created");
                future.complete(answer.sdp);
              }

              @Override
              public void onFailure(String error) {
                future.completeExceptionally(new RuntimeException(error));
              }
            });
          }

          @Override
          public void onFailure(String error) {
            future.completeExceptionally(new RuntimeException(error));
          }
        });
      }

      @Override
      public void onFailure(String error) {
        logger.error("Failed to set remote offer: {}", error);
        future.completeExceptionally(new RuntimeException(error));
      }
    });

    return future;
  }

  /**
   * Handle received SDP answer (leader receives)
   */
  public CompletableFuture<Void> handleAnswer(String sdp) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    RTCSessionDescription answer = new RTCSessionDescription(RTCSdpType.ANSWER, sdp);

    peerConnection.setRemoteDescription(answer, new SetSessionDescriptionObserver() {
      @Override
      public void onSuccess() {
        logger.info("Remote answer set successfully");
        remoteDescriptionSet = true;
        flushPendingRemoteCandidates();
        future.complete(null);
      }

      @Override
      public void onFailure(String error) {
        logger.error("Failed to set remote answer: {}", error);
        future.completeExceptionally(new RuntimeException(error));
      }
    });

    return future;
  }

  /**
   * Add ICE candidate
   */
  public synchronized void addIceCandidate(String candidate, String sdpMid, int sdpMLineIndex) {
    RTCIceCandidate iceCandidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, candidate);

    if (!remoteDescriptionSet) {
      pendingRemoteCandidates.add(iceCandidate);
      logger.info("Buffered ICE candidate until remote description is set");
      return;
    }

    applyIceCandidate(iceCandidate);
  }

  private synchronized void flushPendingRemoteCandidates() {
    if (!remoteDescriptionSet || pendingRemoteCandidates.isEmpty()) {
      return;
    }

    for (RTCIceCandidate candidate : pendingRemoteCandidates) {
      applyIceCandidate(candidate);
    }
    pendingRemoteCandidates.clear();
  }

  private void applyIceCandidate(RTCIceCandidate iceCandidate) {
    if (peerConnection == null) {
      logger.warn("Ignoring ICE candidate because peer connection is not available");
      return;
    }

    peerConnection.addIceCandidate(iceCandidate);
    logger.info("ICE candidate added");
  }

  /**
   * Mute/unmute audio
   */
  public void setMuted(boolean muted) {
    if (localAudioTrack != null) {
      localAudioTrack.setEnabled(!muted);
      logger.info("Audio {}", muted ? "muted" : "unmuted");
    }
  }

  /**
   * Set callback for SDP creation
   */
  public void setOnSdpCreated(Consumer<String> callback) {
    this.onSdpCreated = callback;
  }

  /**
   * Set callback for ICE candidates
   */
  public void setOnIceCandidateGenerated(Consumer<RTCIceCandidate> callback) {
    this.onIceCandidateGenerated = callback;
  }

  /**
   * Cleanup
   */
  public synchronized void dispose() {
    RTCPeerConnection connection = peerConnection;
    RTCRtpSender sender = localAudioSender;
    AudioTrack track = localAudioTrack;
    PeerConnectionFactory currentFactory = factory;
    AudioDeviceModule currentAudioDeviceModule = audioDeviceModule;

    // Null references first to make dispose idempotent and avoid re-entrancy issues.
    peerConnection = null;
    localAudioSender = null;
    localAudioTrack = null;
    factory = null;
    audioDeviceModule = null;
    onSdpCreated = null;
    onIceCandidateGenerated = null;
    remoteDescriptionSet = false;
    pendingRemoteCandidates.clear();

    if (track != null) {
      try {
        track.setEnabled(false);
      } catch (Throwable throwable) {
        logger.debug("Failed to disable local audio track during dispose", throwable);
      }
    }

    if (connection != null && sender != null) {
      try {
        connection.removeTrack(sender);
      } catch (Throwable throwable) {
        logger.debug("Failed to remove sender from peer connection during dispose", throwable);
      }
    }

    if (connection != null) {
      try {
        connection.close();
      } catch (Throwable throwable) {
        logger.warn("Failed to close peer connection during dispose", throwable);
      }
    }

    if (track != null) {
      try {
        track.dispose();
      } catch (Throwable throwable) {
        logger.warn("Failed to dispose local audio track cleanly", throwable);
      }
    }

    if (currentFactory != null) {
      try {
        currentFactory.dispose();
      } catch (Throwable throwable) {
        logger.warn("Failed to dispose peer connection factory cleanly", throwable);
      }
    }

    if (currentAudioDeviceModule != null) {
      try {
        currentAudioDeviceModule.dispose();
      } catch (Throwable throwable) {
        logger.warn("Failed to dispose audio device module cleanly", throwable);
      }
    }

    logger.info("WebRTC disposed");
  }
}
