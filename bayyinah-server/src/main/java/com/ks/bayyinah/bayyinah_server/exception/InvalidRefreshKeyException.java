package com.ks.bayyinah.bayyinah_server.exception;

public class InvalidRefreshKeyException extends RuntimeException {
  public InvalidRefreshKeyException(String message) {
    super(message);
  }

  public InvalidRefreshKeyException() {
    super("Invalid refresh key");
  }
}
