package com.ks.bayyinah.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConfig {
  private String databasePath;
  private String apiUrl;
}
