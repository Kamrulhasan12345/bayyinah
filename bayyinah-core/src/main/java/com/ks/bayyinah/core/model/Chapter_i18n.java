package com.ks.bayyinah.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Chapter_i18n {
  private int id;
  private int chapterId;
  private String langCode;
  private String translatedName;
  private String shortText;
  private String fullText;
}
