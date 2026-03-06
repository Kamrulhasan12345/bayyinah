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

@RestController
@RequestMapping("/api/users")
public class UserController {

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

    currentUser.setFirstName(updatedUser.firstName());
    currentUser.setLastName(updatedUser.lastName());

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

    Optional<UserPreference> userPreferenceOpt = userPreferenceService.findByUserId(currentUser.getId());

    if (userPreferenceOpt.isEmpty()) {
      return ResponseEntity.ok(new UserPreferenceResponse("No preferences found for user", null));
    }

    return ResponseEntity
        .ok(new UserPreferenceResponse("User preferences retrieved successfully", userPreferenceOpt.get()));
  }

  @PutMapping("/me/preferences")

  public ResponseEntity<UserPreferenceResponse> updateUserPreferences(
      @RequestBody UserPreferenceUpdateRequest updatedPreferences) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    Optional<UserPreference> userPreferenceOpt = userPreferenceService.findByUserId(currentUser.getId());

    if (userPreferenceOpt.isEmpty()) {
      UserPreference newPreference = new UserPreference();
      userPreferenceService.save(newPreference);
      return ResponseEntity.ok(new UserPreferenceResponse("User preferences created successfully", newPreference));
    }

    userPreferenceOpt.get().setTheme(updatedPreferences.theme());
    userPreferenceOpt.get().setFontSize(updatedPreferences.fontSize());
    userPreferenceOpt.get().setDefaultTranslation(updatedPreferences.defaultTranslation());
    userPreferenceOpt.get().setLanguage(updatedPreferences.language());
    userPreferenceOpt.get().setReadingMode(updatedPreferences.readingMode());
    userPreferenceOpt.get().setShowTransliteration(updatedPreferences.showTransliteration());
    userPreferenceOpt.get().setAutoScroll(updatedPreferences.autoScroll());

    userPreferenceService.save(userPreferenceOpt.get());

    return ResponseEntity
        .ok(new UserPreferenceResponse("User preferences updated successfully", userPreferenceOpt.get()));
  }
}
