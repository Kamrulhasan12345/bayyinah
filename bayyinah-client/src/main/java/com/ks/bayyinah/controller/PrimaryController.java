package com.ks.bayyinah.controller;

import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.core.query.QuranQueryService;
import com.ks.bayyinah.infra.local.database.DBExecutor;
import com.ks.bayyinah.infra.local.database.DbAsync;
import com.ks.bayyinah.infra.local.query.*;
import java.io.IOException;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class PrimaryController {

  @FXML
  private Text ayahText;

  @FXML
  private void switchToSecondary() throws IOException {
    QuranQueryService quranQueryService = LocalQuranQueryService.getInstance();


    DbAsync.runWithUi(() -> quranQueryService.getVerse("1:1", 20), verse -> {
      verse.ifPresentOrElse(vv -> {
        System.out.println("Verse: " + vv.getArabicText());
        System.out.println("Translation: " + vv.getTranslatedText());
        System.out.println("Verse Key: " + vv);

        ayahText.setText(
            vv.getArabicText() + "\n\n" + vv.getTranslatedText());
      }, () -> {
        System.out.println("Verse not found");
        ayahText.setText("Verse not found");
      });
    });
  }
}
