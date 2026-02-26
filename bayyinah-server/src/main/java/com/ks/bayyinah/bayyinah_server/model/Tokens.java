package com.ks.bayyinah.bayyinah_server.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class Tokens {
  private String accessToken;
  private String refreshToken;
}
