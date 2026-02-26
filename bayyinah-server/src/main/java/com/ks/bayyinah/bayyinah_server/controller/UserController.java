package com.ks.bayyinah.bayyinah_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @GetMapping("/me")
  public ResponseEntity<User> profile() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    return ResponseEntity
        .ok(userDetails.getUser());
  }
}
