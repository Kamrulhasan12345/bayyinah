package com.ks.bayyinah.infra.remote.query;

import java.util.concurrent.CompletableFuture;

import com.ks.bayyinah.infra.remote.client.ApiClient;
import com.ks.bayyinah.infra.remote.dto.recsys.RecommendRequest;
import com.ks.bayyinah.infra.remote.dto.recsys.RecommendResponse;
import com.ks.bayyinah.infra.remote.routing.ApiRoute;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RemoteAIQueryService {

  private final ApiClient apiClient;

  public CompletableFuture<RecommendResponse> getRecommendationsWithReflections(String query, int topK,
      String language) {
    RecommendRequest request = new RecommendRequest(query, topK, language);
    return apiClient.postAi(ApiRoute.AI_RECOMMEND_REFLECT, request, RecommendResponse.class);
  }
}
