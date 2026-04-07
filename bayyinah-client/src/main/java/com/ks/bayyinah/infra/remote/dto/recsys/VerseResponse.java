package com.ks.bayyinah.infra.remote.dto.recsys;

import java.util.List;

public record VerseResponse(
    Integer surah,
    Integer ayah,
    String text,
    String arabic,
    String translationEn,
    String translationUr,
    List<String> emotion,
    List<String> tags,
    List<String> category,
    List<String> context,
    Float relevanceScore,
    Float semanticDistance,
    Float semanticScore,
    Float metadataBoost,
    Float severityPenalty,
    Float repetitionPenalty,
    String reflection, // Equivalent to Optional
    String reflectionProvider // Equivalent to Optional
) {
}
