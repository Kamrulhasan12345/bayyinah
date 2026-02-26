package com.ks.bayyinah.bayyinah_server.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.exception.DuplicateUserException;
import com.ks.bayyinah.bayyinah_server.model.Tokens;
import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.repository.UserRepository;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  AuthenticationManager authManager;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  public User register(User user) {
    if (userRepository.findByUsername(user.getUsername()).isPresent()) {
      throw new DuplicateUserException("Username already exists");
    }

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRepository.save(user);
  }

  public User login(String username, String password) {
    authManager
        .authenticate(new UsernamePasswordAuthenticationToken(username, password));

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    user.setLastLoginAt(LocalDateTime.now());

    userRepository.save(user);

    return user;
  }

  public void logout(String refreshToken) {
    //
    // invalidate the refresh token (e.g., by adding it to a blacklist or removing
    // it from the database)
  }

  public Tokens refreshToken(String refreshToken) {
    // Validate the refresh token and generate a new access token
    String username = jwtService.extractUsername(refreshToken);
    if (jwtService.validateRefreshToken(refreshToken)) {
      String newAccessToken = jwtService.generateToken(username);
      String newRefreshToken = jwtService.generateRefreshToken(username);
      return Tokens.builder()
          .accessToken(newAccessToken)
          .refreshToken(newRefreshToken)
          .build();
    } else {
      throw new RuntimeException("Invalid refresh token");
    }
  }
}
