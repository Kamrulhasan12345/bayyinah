package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConfig {
  private String databasePath;
  private String apiUrl;
}
