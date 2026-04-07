package com.ks.bayyinah.infra.remote.dto.recsys;

public record RecommendRequest(String query, int topK, String language) {
  public RecommendRequest {
    if (language == null || language.isBlank())
      language = "english";
    if (topK <= 0)
      topK = 3;
  }
}
