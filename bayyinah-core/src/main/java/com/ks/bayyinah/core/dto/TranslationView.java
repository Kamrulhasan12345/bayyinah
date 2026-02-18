package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.model.Translation;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TranslationView {
  private int id;
  private Translation translation;
}
