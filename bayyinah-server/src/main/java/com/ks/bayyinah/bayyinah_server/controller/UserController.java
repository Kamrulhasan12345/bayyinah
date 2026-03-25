package com.ks.bayyinah.bayyinah_server.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ks.bayyinah.bayyinah_server.dto.UserDeleteResponse;
import com.ks.bayyinah.bayyinah_server.dto.UserPreferenceResponse;
import com.ks.bayyinah.bayyinah_server.dto.UserPreferenceUpdateRequest;
import com.ks.bayyinah.bayyinah_server.dto.UserUpdateRequest;
import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.model.UserPreference;
import com.ks.bayyinah.bayyinah_server.service.UserPreferenceService;
import com.ks.bayyinah.bayyinah_server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  @Autowired
  private UserService userService;

  @Autowired
  private UserPreferenceService userPreferenceService;

  @GetMapping("/me")
  public ResponseEntity<User> profile() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    return ResponseEntity
        .ok(userDetails.getUser());
  }

  @PutMapping("/me")
  public ResponseEntity<User> updateProfile(@RequestBody UserUpdateRequest updatedUser) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    if (updatedUser.firstName() != null) {
      currentUser.setFirstName(updatedUser.firstName());
    }
    if (updatedUser.lastName() != null) {
      currentUser.setLastName(updatedUser.lastName());
    }

    userService.updateProfile(currentUser);

    return ResponseEntity.ok(currentUser);
  }

  @DeleteMapping("/me")
  public ResponseEntity<UserDeleteResponse> deleteProfile() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    userService.deleteProfile(currentUser.getId());

    return ResponseEntity.ok(new UserDeleteResponse("User profile deleted successfully"));
  }

  @GetMapping("/me/preferences")
  public ResponseEntity<UserPreferenceResponse> getUserPreferences() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    logger.info("User preferences fetch requested. userId={}", currentUser.getId());

    Optional<UserPreference> userPreferenceOpt = userPreferenceService.findByUserId(currentUser.getId());

    if (userPreferenceOpt.isEmpty()) {
      logger.info("User preferences not found. userId={}", currentUser.getId());
      return ResponseEntity.ok(new UserPreferenceResponse("No preferences found for user", null));
    }

    logger.info("User preferences fetched. userId={} preferenceId={}",
        currentUser.getId(), userPreferenceOpt.get().getId());

    return ResponseEntity
        .ok(new UserPreferenceResponse("User preferences retrieved successfully", userPreferenceOpt.get()));
  }

  @PutMapping("/me/preferences")

  public ResponseEntity<UserPreferenceResponse> updateUserPreferences(
      @RequestBody UserPreferenceUpdateRequest updatedPreferences) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    logger.info("User preferences update requested. userId={} theme={} fontSize={} defaultTranslation={} language={} readingMode={}",
      currentUser.getId(), updatedPreferences.theme(), updatedPreferences.fontSize(),
      updatedPreferences.defaultTranslation(), updatedPreferences.language(), updatedPreferences.readingMode());

    Optional<UserPreference> userPreferenceOpt = userPreferenceService.findByUserId(currentUser.getId());

    if (userPreferenceOpt.isEmpty()) {
      UserPreference newPreference = UserPreference.builder()
          .userId(currentUser.getId())
          .theme(updatedPreferences.theme())
          .fontSize(updatedPreferences.fontSize())
          .defaultTranslation(updatedPreferences.defaultTranslation())
          .language(updatedPreferences.language())
          .readingMode(updatedPreferences.readingMode())
          .showTransliteration(updatedPreferences.showTransliteration())
          .autoScroll(updatedPreferences.autoScroll())
          .build();
      userPreferenceService.save(newPreference);
      logger.info("User preferences created. userId={} preferenceId={}", currentUser.getId(), newPreference.getId());
      return ResponseEntity.ok(new UserPreferenceResponse("User preferences created successfully", newPreference));
    }
    UserPreference existingPreference = userPreferenceOpt.get();

    if (updatedPreferences.theme() != null) {
      existingPreference.setTheme(updatedPreferences.theme());
    }
    if (updatedPreferences.fontSize() != null) {
      existingPreference.setFontSize(updatedPreferences.fontSize());
    }
    if (updatedPreferences.defaultTranslation() != null) {
      existingPreference.setDefaultTranslation(updatedPreferences.defaultTranslation());
    }
    if (updatedPreferences.language() != null) {
      existingPreference.setLanguage(updatedPreferences.language());
    }
    if (updatedPreferences.readingMode() != null) {
      existingPreference.setReadingMode(updatedPreferences.readingMode());
    }
    if (updatedPreferences.showTransliteration() != null) {
      existingPreference.setShowTransliteration(updatedPreferences.showTransliteration());
    }
    if (updatedPreferences.autoScroll() != null) {
      existingPreference.setAutoScroll(updatedPreferences.autoScroll());
    }
    userPreferenceService.save(existingPreference);
    logger.info("User preferences updated. userId={} preferenceId={}", currentUser.getId(), existingPreference.getId());

    return ResponseEntity
        .ok(new UserPreferenceResponse("User preferences updated successfully", existingPreference));
  }
}
