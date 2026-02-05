package com.ks.bayyinah.core.models;

public class Chapter {
  private int id;
  private String name_simple;
  private String name_arabic;
  private int verse_count;
  private String revelation_place;

  public Chapter(int id, String name_simple, String name_arabic, int verse_count, String revelation_place) {
    this.id = id;
    this.name_simple = name_simple;
    this.name_arabic = name_arabic;
    this.verse_count = verse_count;
    this.revelation_place = revelation_place;
  }
}
