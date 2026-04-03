package com.ks.bayyinah.infra.remote.query;

import java.util.concurrent.CompletableFuture;

import com.ks.bayyinah.infra.remote.client.ApiClient;
import com.ks.bayyinah.infra.remote.dto.stomp.RoomCreationRequest;
import com.ks.bayyinah.infra.remote.dto.stomp.RoomJoinRequest;
import com.ks.bayyinah.infra.remote.dto.stomp.RoomLeaveRequest;
import com.ks.bayyinah.infra.remote.dto.stomp.RoomResponse;
import com.ks.bayyinah.infra.remote.routing.ApiRoute;

import lombok.*;

@AllArgsConstructor
public class RemoteHalaqahQueryService {
  private final ApiClient apiClient;

  public CompletableFuture<RoomResponse> createRoom(String displayName, Integer maxParticipants) {
    RoomCreationRequest request = new RoomCreationRequest(displayName, maxParticipants);
    return apiClient.post(ApiRoute.HALAQAH_CREATE_ROOM, request, RoomResponse.class);
  }

  public CompletableFuture<RoomResponse> joinRoom(String code, String displayName) {
    RoomJoinRequest request = new RoomJoinRequest(code, displayName);
    return apiClient.post(ApiRoute.HALAQAH_JOIN_ROOM, request, RoomResponse.class);
  }

  public CompletableFuture<Void> leaveRoom(String code) {
    RoomLeaveRequest request = new RoomLeaveRequest(code);
    return apiClient.post(ApiRoute.HALAQAH_LEAVE_ROOM, request, Void.class);
  }

  public CompletableFuture<RoomResponse> getRoom(String code) {
    return apiClient.get(ApiRoute.HALAQAH_ROOM, RoomResponse.class, code);
  }
}
