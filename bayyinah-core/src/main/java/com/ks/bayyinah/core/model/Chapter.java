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
public class Chapter {
  private int id;
  private String nameSimple;
  private String nameArabic;
  private int verseCount;
  private String revelationPlace;
}
