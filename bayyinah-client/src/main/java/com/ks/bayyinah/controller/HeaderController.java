package com.ks.bayyinah.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Meeting header presenter for room context and session status.
 */
public class HeaderController {

  @FXML
  private Label roomCodeLabel;

  @FXML
  private Label roleLabel;

  @FXML
  private Label leaderLabel;

  @FXML
  private Label participantsLabel;

  @FXML
  private Label verseFocusLabel;

  @FXML
  private Label timerLabel;

  @FXML
  private Label connectionStateLabel;

  public void setRoomCode(String roomCode) {
    roomCodeLabel.setText("Room: " + safe(roomCode, "-"));
  }

  public void setRole(boolean isLeader) {
    roleLabel.setText("Role: " + (isLeader ? "Leader" : "Participant"));
  }

  public void setLeader(String leaderName) {
    leaderLabel.setText("Leader: " + safe(leaderName, "-"));
  }

  public void setParticipantCount(int count) {
    participantsLabel.setText("Participants: " + Math.max(0, count));
  }

  public void setVerseFocus(int surah, int ayah) {
    verseFocusLabel.setText("Focus: Surah " + Math.max(1, surah) + ", Ayah " + Math.max(1, ayah));
  }

  public void setTimer(String value) {
    timerLabel.setText(value == null || value.isBlank() ? "00:00" : value);
  }

  public void setConnectionState(String state) {
    connectionStateLabel.setText(state == null || state.isBlank() ? "Offline" : state);
  }

  private String safe(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }
}
