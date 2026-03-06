package com.ks.bayyinah.bayyinah_server.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.model.UserPreference;
import com.ks.bayyinah.bayyinah_server.repository.UserPreferenceRepository;

@Service
public class UserPreferenceService {
  @Autowired
  private UserPreferenceRepository userPreferenceRepository;

  public Optional<UserPreference> findByUserId(Long userId) {
    return userPreferenceRepository.findByUserId(userId);
  }

  public UserPreference save(UserPreference userPreference) {
    return userPreferenceRepository.save(userPreference);
  }
}
