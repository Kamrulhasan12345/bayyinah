package com.ks.bayyinah;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.core.models.*;
import com.ks.bayyinah.core.repository.*;

public class PrimaryController {
  @FXML
  private Text ayahText;

  @FXML
  private void switchToSecondary() throws IOException {
    // App.setRoot("secondary");

    QuranRepository repository = new LocalQuranRepository("E:/bayyinah/data/quran.db");

    repository.getVerseByKey("1:1").ifPresent(vv -> {
      System.out.println("Verse: " + vv.getText());

      ayahText.setText(vv.getText() + "\n\n" + vv.getVerseKey());
    });
  }
}
