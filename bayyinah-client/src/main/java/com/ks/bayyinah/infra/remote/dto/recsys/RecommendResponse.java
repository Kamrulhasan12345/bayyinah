package com.ks.bayyinah.infra.remote.dto.recsys;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

public record RecommendResponse(
    String query,
    List<String> detectedEmotions,
    List<VerseResponse> verses,
    JsonNode metadata) {
}
