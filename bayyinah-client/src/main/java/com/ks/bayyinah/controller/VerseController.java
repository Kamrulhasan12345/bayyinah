package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Verse;
import com.ks.bayyinah.core.model.TranslationText;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class VerseController {

  @FXML
  private Label arabicText;

  @FXML
  private Label translatedText;

  @FXML
  private Label translationMetadata;

  @FXML
  private Label verseNumber;

  private VerseView verse;

  public void bind(VerseView verse) {
    this.verse = verse;

    Verse verseData = verse.getVerse();
    TranslationText translationText = verse.getTranslationText();

    verseNumber.setText(String.valueOf(verseData.getVerseNumber()));
    arabicText.setText(verse.getArabicText());
    translatedText.setText(translationText.getText());
    translationMetadata.setText(
        String.format(
            "%s - %s",
            "Sahih International",
            "en"));

    // TODO: maybe some other buttons .setOnAction(e -> { ... }) for bookmarking,
    // etc.
  }
}
