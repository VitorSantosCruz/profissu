package br.com.conectabyte.profissu.config.interceptors;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.repositories.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthChannelInterceptor implements ChannelInterceptor {
  private final JwtDecoder jwtDecoder;
  private final ConversationRepository conversationRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    log.debug("Intercepting STOMP message: {}", message.getHeaders());

    final var accessor = StompHeaderAccessor.wrap(message);
    final var token = accessor.getFirstNativeHeader("token");
    final var decodedToken = validateToken(token);

    if (decodedToken == null) {
      log.warn("STOMP message rejected: Invalid or missing JWT token.");
      return null;
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
      final var destination = accessor.getDestination();
      final var conversationId = extractConversationId(destination);
      final var userId = Optional.ofNullable(decodedToken.getClaims().get("sub"))
          .map(Object::toString)
          .map(Long::valueOf)
          .orElseThrow(() -> new ValidationException("User ID not found in JWT claims."));

      log.debug("STOMP command: {}, Destination: {}, Conversation ID: {}, User ID: {}",
          accessor.getCommand(), destination, conversationId, userId);

      if (conversationId == null) {
        log.warn("STOMP message rejected: Could not extract conversation ID from destination: {}", destination);
        return null;
      }

      if (!conversationRepository.isUserInConversation(userId, conversationId)) {
        log.warn("User ID {} is not authorized for conversation ID {} in STOMP command {}.", userId, conversationId,
            accessor.getCommand());
        return null;
      }

      log.debug("User ID {} is authorized for conversation ID {} in STOMP command {}.", userId, conversationId,
          accessor.getCommand());
    }

    log.debug("STOMP message allowed to proceed.");
    return message;
  }

  private Jwt validateToken(String token) {
    log.debug("Validating JWT token.");

    try {
      if (token == null || token.isBlank()) {
        log.warn("JWT token is missing or blank.");
        throw new ValidationException("JWT token is required for this action.");
      }

      if (token.startsWith("Bearer ")) {
        log.debug("Removing 'Bearer ' prefix from token.");
        token = token.substring(7);
      }

      final var jwt = jwtDecoder.decode(token);

      log.debug("JWT token decoded successfully for subject: {}", jwt.getSubject());
      return jwt;
    } catch (Exception e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
      return null;
    }
  }

  private Long extractConversationId(String destination) {
    log.debug("Attempting to extract conversation ID from destination: {}", destination);

    final var pattern = Pattern.compile("^/topic/conversations/(\\d+)/messages$");
    final var matcher = pattern.matcher(destination);

    if (matcher.matches()) {
      final Long conversationId = Long.valueOf(matcher.group(1));

      log.debug("Extracted conversation ID: {} from destination: {}", conversationId, destination);
      return conversationId;
    }

    log.warn("Could not extract conversation ID. Destination pattern not matched: {}", destination);
    return null;
  }
}
