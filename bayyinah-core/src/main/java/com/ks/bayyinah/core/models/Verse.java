package com.ks.bayyinah.core.models;

public class Verse {
  private int id;
  private int surah_id;
  private int verse_number;
  private String verse_key;
  private String text_uthmani;
  private String text_indopak;

  public Verse(int id, int surah_id, int verse_number, String verse_key, String text_uthmani,
      String text_indopak) {
    this.id = id;
    this.surah_id = surah_id;
    this.verse_number = verse_number;
    this.verse_key = verse_key;
    this.text_uthmani = text_uthmani;
    this.text_indopak = text_indopak;
  }
};
