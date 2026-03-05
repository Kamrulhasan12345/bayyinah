package com.ks.bayyinah.infra.hybrid.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiConfig {
  private int connectTimeoutSeconds;
  private int requestTimeoutSeconds;

  private int maxRetries;
  private int retryDelayMs;
}
