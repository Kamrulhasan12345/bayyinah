package com.ks.bayyinah;

import java.io.IOException;
import javafx.fxml.FXML;
import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.core.models.*;

public class PrimaryController {

  @FXML
  private void switchToSecondary() throws IOException {
    App.setRoot("secondary");
    Verse verse = new Verse(1, 1, 1, "1:1",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ");
    Translation translation = new Translation(1, "Sahih International", "en");
    VerseView verseView = new VerseView(verse, translation);
  }
}
