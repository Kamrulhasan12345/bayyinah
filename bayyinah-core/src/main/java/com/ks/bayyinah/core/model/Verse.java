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
public class Verse {
  private int id;
  private int surahId;
  private int verseNumber;
  private String verseKey;
  private String textUthmani;
  private String textIndopak;

  public String getText() {
    return this.textUthmani != null ? this.textUthmani : this.textIndopak;
  }
};
