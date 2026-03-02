module com.ks.bayyinah {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.sql;
  requires com.zaxxer.hikari;
  requires jakarta.persistence;

  requires tools.jackson.core;
  requires tools.jackson.databind;
  requires tools.jackson.dataformat.yaml;

  requires bayyinah.core;
  requires lombok;
  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.ikonli.materialdesign2;

  opens com.ks.bayyinah to javafx.fxml;
  opens com.ks.bayyinah.controller to javafx.fxml;
  opens com.ks.bayyinah.infra.hybrid.model to tools.jackson.databind;

  exports com.ks.bayyinah.controller;
  exports com.ks.bayyinah;
}
