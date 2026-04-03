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
  private AudioTrack localAudioTrack;

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
    try {
      // Initialize audio device module
      // TODO: this is where i get to hook it up to any dropdown stuff to get a
      // "configured" audioDeviceModule
      audioDeviceModule = new AudioDeviceModule();

      // Create peer connection factory
      factory = new PeerConnectionFactory(audioDeviceModule);

      logger.info("WebRTC initialized");

    } catch (Exception e) {
      logger.error("Failed to initialize WebRTC", e);
      throw new RuntimeException("WebRTC initialization failed", e);
    }
  }

  /**
   * Create peer connection with STUN server
   */
  public void createPeerConnection(boolean isLeader) {
    this.isLeader = isLeader;

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
   * Start audio capture (leader only)
   */
  public void startAudioCapture() {
    if (!isLeader) {
      logger.warn("Only leader can capture audio");
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
      peerConnection.addTrack(localAudioTrack, List.of("stream-" + userId));

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
  public void handleAnswer(String sdp) {
    RTCSessionDescription answer = new RTCSessionDescription(RTCSdpType.ANSWER, sdp);

    peerConnection.setRemoteDescription(answer, new SetSessionDescriptionObserver() {
      @Override
      public void onSuccess() {
        logger.info("Remote answer set successfully");
      }

      @Override
      public void onFailure(String error) {
        logger.error("Failed to set remote answer: {}", error);
      }
    });
  }

  /**
   * Add ICE candidate
   */
  public void addIceCandidate(String candidate, String sdpMid, int sdpMLineIndex) {
    RTCIceCandidate iceCandidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, candidate);

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
  public void dispose() {
    if (localAudioTrack != null) {
      localAudioTrack.dispose();
    }

    if (peerConnection != null) {
      peerConnection.close();
    }

    if (factory != null) {
      factory.dispose();
    }

    logger.info("WebRTC disposed");
  }
}
