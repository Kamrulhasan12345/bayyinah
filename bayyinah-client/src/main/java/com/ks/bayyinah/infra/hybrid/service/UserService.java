package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.hybrid.model.User;
import com.ks.bayyinah.infra.local.repository.user.UserRepository;

public class UserService {
  private final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  public User getCurrentUser() {
    return repository.get();
  }

  public void saveUser(User user) {
    if (repository.get() != null) {
      repository.update(user);
    } else {
      repository.insert(user);
    }
  }

  public void updateUser(User updatedUser) {
    repository.update(updatedUser);
  }

  public void clearUser() {
    repository.clear();
  }

  public boolean isGuest() {
    User user = repository.get();
    return user == null || user.isGuest();
  }

  public void createGuestUser() {
    User guestUser = User.createGuest();
    saveUser(guestUser);
  }
}
