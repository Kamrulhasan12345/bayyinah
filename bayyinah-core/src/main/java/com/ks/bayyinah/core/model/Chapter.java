package com.ks.bayyinah.core.model;

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

  public int getId() {
    return id;
  }

  public String getNameSimple() {
    return name_simple;
  }

  public String getNameArabic() {
    return name_arabic;
  }

  public int getVerseCount() {
    return verse_count;
  }

  public String getRevelationPlace() {
    return revelation_place;
  }

  @Override
  public String toString() {
    return "Chapter{" +
        "id=" + id +
        ", name_simple='" + name_simple + '\'' +
        ", name_arabic='" + name_arabic + '\'' +
        ", verse_count=" + verse_count +
        ", revelation_place='" + revelation_place + '\'' +
        '}';
  }
}
