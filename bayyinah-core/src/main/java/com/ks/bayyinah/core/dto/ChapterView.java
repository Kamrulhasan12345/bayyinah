package com.ks.bayyinah.core.dto;

import com.ks.bayyinah.core.model.Chapter;
import com.ks.bayyinah.core.model.Chapter_i18n;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChapterView {
  private int id;
  private Chapter chapter;
  private Chapter_i18n chapterI18N;
}
