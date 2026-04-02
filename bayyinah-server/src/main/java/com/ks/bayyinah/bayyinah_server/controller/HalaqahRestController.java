package com.ks.bayyinah.bayyinah_server.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ks.bayyinah.bayyinah_server.dto.*;
import com.ks.bayyinah.bayyinah_server.dto.stomp.Participant;
import com.ks.bayyinah.bayyinah_server.dto.stomp.Room;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.service.RoomService;

@RestController
@RequestMapping("/api/halaqah")
public class HalaqahRestController {

  @Autowired
  private RoomService roomService;

  @PostMapping("/create")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RoomResponse> createRoom(
      @RequestBody RoomCreationRequest request,
      Authentication auth) {
    String userId = ((UserDetailsImpl) auth.getPrincipal()).getUser().getId().toString();

    Participant leader = new Participant(userId, request.displayName(), LocalDateTime.now(), true, false);

    Room room = roomService.createRoom(
        leader,
        request.maxParticipants());

    return ResponseEntity.ok(RoomResponse.from(room));
  }

  @PostMapping("/join")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<RoomResponse> joinRoom(
      @RequestBody RoomJoinRequest request,
      Authentication auth) {
    String userId = ((UserDetailsImpl) auth.getPrincipal()).getUser().getId().toString();

    Room room = roomService.getRoom(request.code())
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));

    Participant participant = room.getParticipants().get(userId);

    try {
      if (participant != null) {
        throw new IllegalStateException(
            "You can only join a room once.");
      } else if (participant == null) {
        participant = new Participant(userId, request.displayName(), LocalDateTime.now(), false, false);
      }
      Room roomResp = roomService.joinRoom(request.code(), participant);
      return ResponseEntity.ok(RoomResponse.from(roomResp));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PostMapping("/leave/{roomId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> leaveRoom(
      @PathVariable String roomId,
      Authentication auth) {
    String userId = ((UserDetailsImpl) auth.getPrincipal()).getUser().getId().toString();

    try {

      Room room = roomService.getRoom(roomId)
          .orElseThrow(() -> new IllegalArgumentException("Room not found"));

      Participant participant = room.getParticipants().get(userId);
      if (participant == null) {
        throw new IllegalArgumentException("User not found in room");
      }
      roomService.leaveRoom(roomId, participant);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/{code}")
  public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
    return roomService.getRoom(code)
        .map(room -> ResponseEntity.ok(RoomResponse.from(room)))
        .orElse(ResponseEntity.notFound().build());
  }
}
