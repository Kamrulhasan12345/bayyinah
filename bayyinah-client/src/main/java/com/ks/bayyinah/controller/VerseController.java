package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Verse;
import com.ks.bayyinah.infra.hybrid.model.Bookmark;
import com.ks.bayyinah.infra.hybrid.service.BookmarkService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.core.model.TranslationText;
import com.ks.bayyinah.context.AppContext;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Optional;

import org.kordamp.ikonli.javafx.FontIcon;

public class VerseController {

  @FXML
  private Label arabicText;

  @FXML
  private Label translatedText;

  @FXML
  private Label translationMetadata;

  @FXML
  private Label verseNumber;

  @FXML
  private FontIcon bookmarkBtn;

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

    AppContext appContext = AppContext.getInstance();
    BookmarkService bookmarkService = appContext.getBookmarkService();
    Optional<Bookmark> bookmark = bookmarkService.getByVerse(verse.getVerse().getSurahId(),
        verse.getVerse().getVerseNumber());

    if (bookmark.isPresent()) {
      bookmarkBtn.setIconLiteral("mdi2b-bookmark");
    } else {
      bookmarkBtn.setIconLiteral("mdi2b-bookmark-outline");
    }

    bookmarkBtn.setOnMouseClicked(e -> {
      DbAsync.runWithUi(() -> bookmarkService.getByVerse(verse.getVerse().getSurahId(),
          verse.getVerse().getVerseNumber()), bookmarkB -> {
            if (bookmarkB.isPresent()) {
              DbAsync.runWithUi(() -> {
                bookmarkService.removeBookmark(bookmarkB.get().getId());
                return null;
              }, ignored -> bookmarkBtn.setIconLiteral("mdi2b-bookmark-outline"));
            } else {
              DbAsync.runWithUi(
                  () -> {
                    bookmarkService.addBookmark(verse.getVerse().getSurahId(), verse.getVerse().getVerseNumber());
                    return null;
                  },
                  ignored -> bookmarkBtn.setIconLiteral("mdi2b-bookmark"));
            }
          });
    });
  }
}
