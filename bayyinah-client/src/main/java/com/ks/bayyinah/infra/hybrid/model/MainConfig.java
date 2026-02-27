package com.ks.bayyinah.infra.hybrid.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainConfig {
  private QuranConfig quran;
  private UserConfig user;
  private String apiUrl;
}
