package com.ks.bayyinah.bayyinah_server.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketExceptionHandler.class);

  @MessageExceptionHandler(SecurityException.class)
  @SendToUser("/queue/errors")
  public ErrorMessage handleSecurityException(SecurityException e) {
    logger.error("Security error: {}", e.getMessage());
    return new ErrorMessage("SECURITY_ERROR", e.getMessage());
  }

  @MessageExceptionHandler(IllegalArgumentException.class)
  @SendToUser("/queue/errors")
  public ErrorMessage handleIllegalArgument(IllegalArgumentException e) {
    logger.error("Validation error: {}", e.getMessage());
    return new ErrorMessage("VALIDATION_ERROR", e.getMessage());
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser("/queue/errors")
  public ErrorMessage handleGenericException(Exception e) {
    logger.error("Unexpected error", e);
    return new ErrorMessage("SERVER_ERROR", "An error occurred");
  }

  public record ErrorMessage(String type, String message) {
  }
}
