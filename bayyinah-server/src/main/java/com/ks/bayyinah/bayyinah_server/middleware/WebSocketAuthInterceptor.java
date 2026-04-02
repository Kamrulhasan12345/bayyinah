package com.ks.bayyinah.bayyinah_server.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.service.JwtService;
import com.ks.bayyinah.bayyinah_server.service.UserDetailsServiceImpl;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

  @Autowired
  private JwtService jwtService;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;


  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authHeader = accessor.getFirstNativeHeader("Authorization");

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        logger.warn("WebSocket CONNECT without Authorization header");
        throw new SecurityException("Missing or invalid Authorization header");
      }

      String token = authHeader.substring(7);

      try {
        String username = jwtService.extractUsername(token);

        // DoS protection
        UserDetailsImpl userDetails = userDetailsService.loadUserByUsername(username);
        if (jwtService.validateToken(token, userDetails)) {
          String userId = userDetails.getUser().getId().toString();
          // if (!connectionManager.canConnect(userId)) {
          // throw new SecurityException("Maximum connections exceeded");
          // }
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());
          accessor.setUser(authToken);
          SecurityContextHolder.getContext().setAuthentication(authToken);

          // connectionManager.registerConnection(accessor.getSessionId(), userId,
          // username);

          logger.info("WebSocket authenticated: userId={}", userId);
        } else {
          throw new SecurityException("Invalid token");
        }

      } catch (Exception e) {
        logger.error("WebSocket auth failed: {}", e.getMessage());
        throw new SecurityException("Authentication failed: " + e.getMessage());
      }

    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
      // connectionManager.removeConnection(accessor.getSessionId());
      SecurityContextHolder.clearContext();
    }

    return message;
  }
}
