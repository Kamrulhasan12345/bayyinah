package com.ks.bayyinah.bayyinah_server.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) {
    Optional<User> user = userRepository.findByUsername(username);
    if (user.isPresent()) {
      return new UserDetailsImpl(user.get());
    } else {
      throw new UsernameNotFoundException("User not found with username: " + username);
    }
  }
}
