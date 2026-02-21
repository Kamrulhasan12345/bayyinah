package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.ChapterView;

public class ChaptersController {
  private BrowsingController browsingController;

  public void setBrowsingController(BrowsingController browsingController) {
    this.browsingController = browsingController;
  }

  public void setChapter(ChapterView chapter) {
    System.out.println("Loading chapter: " + chapter.toString());
  }

}
