package com.ks.bayyinah.bayyinah_server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.*;

import com.ks.bayyinah.bayyinah_server.dto.*;
import com.ks.bayyinah.bayyinah_server.model.Tokens;
import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.service.JwtService;
import com.ks.bayyinah.bayyinah_server.service.UserService;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationController {

  @Autowired
  private JwtService jwtService;

  @Autowired
  private UserService userService;

  @PostMapping("/register")
  public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
    User requestUser = User.builder()
        .username(request.username())
        .email(request.email())
        .firstName(request.firstName())
        .lastName(request.lastName())
        .password(request.password())
        .role(request.role())
        .build();

    User user = userService.register(requestUser);

    return ResponseEntity.ok(new RegistrationResponse("User registered successfully", user));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    User authenticatedUser = userService.login(request.username(), request.password());

    String accessToken = jwtService.generateToken(authenticatedUser.getUsername());
    String refreshToken = jwtService.generateRefreshToken(authenticatedUser.getUsername());

    Tokens tokens = Tokens.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();

    return ResponseEntity.ok(new LoginResponse("Successfully logged in", authenticatedUser, tokens));
  }

  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest request) {
    userService.logout(request.refreshToken());
    return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
  }

  @PostMapping("/refresh")
  public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
    Tokens newTokens = userService.refreshToken(request.refreshToken());
    return ResponseEntity.ok(new RefreshTokenResponse("Token refreshed successfully", newTokens));
  }

  // @PostMapping("/forgot-password")
  // public ResponseEntity<ForgotPasswordResponse> forgotPassword() {
  // return ResponseEntity.ok("Password reset link sent to your email");
  // }
  //
  // @PostMapping("/reset-password")
  // public ResponseEntity<ResetPasswordResponse> resetPassword() {
  // return ResponseEntity.ok("Password reset successfully");
  // }
  //
  // @PostMapping("/verify-email")
  // public ResponseEntity<VerifyEmailResponse> verifyEmail() {
  // return ResponseEntity.ok("Email verified successfully");
  // }
}
