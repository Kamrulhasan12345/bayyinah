module com.ks.bayyinah {
  requires javafx.controls;
  requires javafx.fxml;

  requires bayyinah.core;

  opens com.ks.bayyinah to javafx.fxml;
  opens com.ks.bayyinah.controllers to javafx.fxml;

  exports com.ks.bayyinah.controllers;
  exports com.ks.bayyinah;
}
