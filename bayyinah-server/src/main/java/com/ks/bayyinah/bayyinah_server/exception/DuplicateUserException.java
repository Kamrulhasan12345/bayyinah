package com.ks.bayyinah.bayyinah_server.exception;

public class DuplicateUserException extends RuntimeException {
  public DuplicateUserException(String message) {
    super(message);
  }

  public DuplicateUserException() {
    super("User already exists");
  }
}
