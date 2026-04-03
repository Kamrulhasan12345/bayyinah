module com.ks.bayyinah {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.sql;
  requires java.net.http;
  requires com.zaxxer.hikari;
  requires jakarta.persistence;
  requires org.slf4j;
  requires fr.brouillard.oss.cssfx;
  requires spring.core;
  requires spring.beans;
  requires spring.web;
  requires spring.context;
  requires spring.websocket;
  requires spring.messaging;
  requires webrtc.java;

  requires tools.jackson.core;
  requires tools.jackson.databind;
  requires tools.jackson.dataformat.yaml;

  requires bayyinah.core;
  requires lombok;
  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.ikonli.materialdesign2;
  requires javafx.graphics;

  opens com.ks.bayyinah to javafx.fxml;
  opens com.ks.bayyinah.controller to javafx.fxml;
  opens com.ks.bayyinah.infra.hybrid.model to tools.jackson.databind;
  opens com.ks.bayyinah.infra.hybrid.service to tools.jackson.databind;
  opens com.ks.bayyinah.infra.remote.dto.auth to tools.jackson.databind;
  opens com.ks.bayyinah.infra.remote.dto.sync to tools.jackson.databind;
  opens com.ks.bayyinah.infra.remote.dto.stomp to tools.jackson.databind;
  opens com.ks.bayyinah.infra.remote.client to spring.core, spring.beans, spring.context;

  exports com.ks.bayyinah.controller;
  exports com.ks.bayyinah;
}
