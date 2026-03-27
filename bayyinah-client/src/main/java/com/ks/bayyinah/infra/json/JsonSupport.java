package com.ks.bayyinah.infra.json;

import tools.jackson.databind.ObjectMapper;

public final class JsonSupport {
  private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper();

  private JsonSupport() {
  }

  public static ObjectMapper objectMapper() {
    return SHARED_OBJECT_MAPPER;
  }
}