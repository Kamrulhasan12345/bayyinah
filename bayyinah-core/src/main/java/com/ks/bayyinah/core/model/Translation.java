package com.ks.bayyinah.core.model;

public class Translation {
  private int id;
  private String author_name;
  private String language;

  public Translation(int id, String author_name, String language) {
    this.id = id;
    this.author_name = author_name;
    this.language = language;
  }

  public int getId() {
    return this.id;
  }

  public String getAuthorName() {
    return this.author_name;
  }

  public String getLanguage() {
    return this.language;
  }

  public String getName() {
    return this.author_name + " (" + this.language + ")";
  }

  @Override
  public String toString() {
    return "Translation{id=" + id + ", author_name='" + author_name + "', language='" + language + "'}";
  }
}
