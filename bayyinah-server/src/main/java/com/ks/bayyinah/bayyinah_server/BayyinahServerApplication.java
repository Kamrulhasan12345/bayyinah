package com.ks.bayyinah.bayyinah_server;

import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@RestController
@SpringBootApplication
@EnableScheduling
public class BayyinahServerApplication {

  @RequestMapping("/health")
  String home() {
    return "Apps Working fine!";
  }

  public static void main(String[] args) {
    SpringApplication.run(BayyinahServerApplication.class, args);
  }

}
