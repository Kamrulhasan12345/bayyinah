module com.ks.bayyinah {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.sql;
  requires com.zaxxer.hikari;
  requires jakarta.persistence;

  requires bayyinah.core;
  requires lombok;

  opens com.ks.bayyinah to javafx.fxml;
  opens com.ks.bayyinah.controller to javafx.fxml;
  // opens com.ks.bayyinah.infra.remote to com.fasterxml.jackson.databind;

  exports com.ks.bayyinah.controller;
  exports com.ks.bayyinah;
}
