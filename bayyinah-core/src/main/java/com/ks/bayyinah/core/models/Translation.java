package com.ks.bayyinah.core.models;

public class Translation {
  private int id;
  private String author_name;
  private String language;

  public Translation(int id, String author_name, String language) {
    this.id = id;
    this.author_name = author_name;
    this.language = language;
  }
}
