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
public class TranslationText {
  private int id;
  private int verseId;
  private int translationId;
  private String text;
}
