package com.ks.bayyinah.bayyinah_server;

import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@RestController
@SpringBootApplication
public class BayyinahServerApplication {

  @RequestMapping("/")
  String home() {
    return "Hello World!";
  }

  public static void main(String[] args) {
    SpringApplication.run(BayyinahServerApplication.class, args);
  }

}
