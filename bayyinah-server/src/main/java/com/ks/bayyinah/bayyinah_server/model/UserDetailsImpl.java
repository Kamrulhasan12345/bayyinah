package com.ks.bayyinah.bayyinah_server.model;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.*;

@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
  @Getter
  private User user;

  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority(user.getRole()));
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    // No explicit account expiration field; treat accounts as non-expired.
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    // Treat null or false as "not locked".
    return !Boolean.TRUE.equals(user.getAccountLocked());
  }

  @Override
  public boolean isCredentialsNonExpired() {
    // No explicit credential expiration field; treat credentials as non-expired.
    return true;
  }

  @Override
  public boolean isEnabled() {
    // Null-safe check; only TRUE is considered enabled.
    return Boolean.TRUE.equals(user.getEnabled());
  }
}
