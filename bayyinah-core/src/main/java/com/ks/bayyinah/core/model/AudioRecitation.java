package com.ks.bayyinah.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AudioRecitation {
  private int id;
  private String reciterName;
  private String style;
  private String translatedName;
}
