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
    // FIXME: just a temp thing
    return user.getEnabled();
  }

  @Override
  public boolean isAccountNonLocked() {
    return user.getAccountLocked() == false;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    // FIXME: a temp thing
    return user.getEnabled();
  }

  @Override
  public boolean isEnabled() {
    return user.getEnabled();
  }
}
