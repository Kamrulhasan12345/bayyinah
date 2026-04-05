package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.VerseView;
import com.ks.bayyinah.core.model.Translation;
import com.ks.bayyinah.core.model.Verse;
import com.ks.bayyinah.infra.hybrid.model.Bookmark;
import com.ks.bayyinah.infra.hybrid.service.BookmarkService;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.core.model.TranslationText;
import com.ks.bayyinah.context.AppContext;
import com.ks.bayyinah.infra.local.repository.quran.LocalTranslationRepository;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.kordamp.ikonli.javafx.FontIcon;

public class VerseController {

  private static final Map<Integer, String> TRANSLATION_AUTHOR_CACHE = new ConcurrentHashMap<>();
  private static final Map<Integer, String> TRANSLATION_LANGUAGE_CACHE = new ConcurrentHashMap<>();
  private static final Set<Integer> TRANSLATION_FETCH_IN_FLIGHT = ConcurrentHashMap.newKeySet();

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

  @FXML
  private Button syncVerseBtn;

  private VerseView verse;

  public void bind(VerseView verse) {
    bind(verse, null);
  }

  public void bind(VerseView verse, Consumer<VerseView> onVerseSyncRequested) {
    this.verse = verse;

    Verse verseData = verse.getVerse();
    TranslationText translationText = verse.getTranslationText();

    verseNumber.setText(String.valueOf(verseData.getVerseNumber()));
    arabicText.setText(verse.getArabicText());
    translatedText.setText(translationText != null && translationText.getText() != null
      ? translationText.getText()
      : "");
    translationMetadata.setText(buildTranslationMetadata(translationText));
    requestTranslationMetadataAsync(translationText, verseData.getVerseKey());

    AppContext appContext = AppContext.getInstance();
    BookmarkService bookmarkService = appContext.getBookmarkService();
    int surahId = verse.getVerse().getSurahId();
    int verseNumberValue = verse.getVerse().getVerseNumber();
    String boundVerseKey = verseData.getVerseKey();

    bookmarkBtn.setIconLiteral("mdi2b-bookmark-outline");
    DbAsync.runWithUi(() -> bookmarkService.getByVerse(surahId, verseNumberValue), bookmark -> {
      if (this.verse == null || this.verse.getVerse() == null) {
        return;
      }
      String currentVerseKey = this.verse.getVerse().getVerseKey();
      if (boundVerseKey != null && !boundVerseKey.equals(currentVerseKey)) {
        return;
      }

      if (bookmark.isPresent()) {
        bookmarkBtn.setIconLiteral("mdi2b-bookmark");
      } else {
        bookmarkBtn.setIconLiteral("mdi2b-bookmark-outline");
      }
    });

    bookmarkBtn.setOnMouseClicked(e -> {
      DbAsync.runWithUi(() -> bookmarkService.getByVerse(surahId, verseNumberValue), bookmarkB -> {
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

    configureVerseSyncButton(onVerseSyncRequested);
  }

  private void configureVerseSyncButton(Consumer<VerseView> onVerseSyncRequested) {
    if (syncVerseBtn == null) {
      return;
    }

    boolean enabled = onVerseSyncRequested != null;
    syncVerseBtn.setVisible(enabled);
    syncVerseBtn.setManaged(enabled);
    syncVerseBtn.setDisable(!enabled);

    if (!enabled) {
      syncVerseBtn.setOnAction(null);
      return;
    }

    syncVerseBtn.setOnAction(event -> {
      if (verse != null && verse.getVerse() != null) {
        onVerseSyncRequested.accept(verse);
      }
    });
  }

  private String buildTranslationMetadata(TranslationText translationText) {
    if (translationText == null || translationText.getTranslationId() <= 0) {
      return "Translation - N/A";
    }

    int translationId = translationText.getTranslationId();
    String authorName = TRANSLATION_AUTHOR_CACHE.get(translationId);
    String language = TRANSLATION_LANGUAGE_CACHE.get(translationId);

    if (authorName == null || authorName.isBlank()) {
      authorName = "Translation #" + translationId;
    }

    if (language == null || language.isBlank()) {
      language = "english";
    }

    return String.format("%s - %s", authorName, language);
  }

  private void requestTranslationMetadataAsync(TranslationText translationText, String boundVerseKey) {
    if (translationText == null || translationText.getTranslationId() <= 0) {
      return;
    }

    int translationId = translationText.getTranslationId();
    String cachedAuthor = TRANSLATION_AUTHOR_CACHE.get(translationId);
    String cachedLanguage = TRANSLATION_LANGUAGE_CACHE.get(translationId);
    if (cachedAuthor != null && !cachedAuthor.isBlank() && cachedLanguage != null && !cachedLanguage.isBlank()) {
      return;
    }

    if (!TRANSLATION_FETCH_IN_FLIGHT.add(translationId)) {
      return;
    }

    DbAsync.runWithUi(
        () -> {
          LocalTranslationRepository translationRepository = new LocalTranslationRepository();
          return translationRepository.findTranslationById(translationId);
        },
        translation -> {
          TRANSLATION_FETCH_IN_FLIGHT.remove(translationId);
          translation.ifPresent(this::cacheTranslationMetadata);

          if (verse == null || verse.getVerse() == null || verse.getTranslationText() == null) {
            return;
          }

          String currentVerseKey = verse.getVerse().getVerseKey();
          int currentTranslationId = verse.getTranslationText().getTranslationId();
          if (boundVerseKey != null && boundVerseKey.equals(currentVerseKey) && currentTranslationId == translationId) {
            translationMetadata.setText(buildTranslationMetadata(verse.getTranslationText()));
          }
        },
        err -> {
          TRANSLATION_FETCH_IN_FLIGHT.remove(translationId);
          err.printStackTrace();
        });
  }

  private void cacheTranslationMetadata(Translation translation) {
    String authorName = translation.getAuthorName();
    String language = translation.getLanguage();

    if (authorName != null && !authorName.isBlank()) {
      TRANSLATION_AUTHOR_CACHE.put(translation.getId(), authorName);
    }
    if (language != null && !language.isBlank()) {
      TRANSLATION_LANGUAGE_CACHE.put(translation.getId(), language);
    }
  }
}
