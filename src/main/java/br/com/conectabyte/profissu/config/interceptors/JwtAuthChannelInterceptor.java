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
    final var accessor = StompHeaderAccessor.wrap(message);
    final var token = accessor.getFirstNativeHeader("token");
    final var decodedToken = validateToken(token);

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
      final var destination = accessor.getDestination();
      final var conversationId = extractConversationId(destination);
      final var userId = Optional.ofNullable(decodedToken.getClaims().get("sub"))
          .map(Object::toString)
          .map(Long::valueOf)
          .orElseThrow();

      if (decodedToken == null || conversationId == null) {
        return null;
      }

      if (!conversationRepository.isUserInConversation(userId, conversationId)) {
        log.warn("User is not authorized for this subscription.");

        return null;
      }
    }

    return message;
  }

  private Jwt validateToken(String token) {
    try {
      if (token == null || token.isBlank()) {
        throw new ValidationException("JWT token is required for this action.");
      }

      if (token.startsWith("Bearer ")) {
        token = token.substring(7);
      }

      return jwtDecoder.decode(token);
    } catch (Exception e) {
      log.warn("Invalid JWT token: " + e.getMessage());

      return null;
    }
  }

  private Long extractConversationId(String destination) {
    final var pattern = Pattern.compile("^/topic/conversations/(\\d+)/messages$");
    final var matcher = pattern.matcher(destination);

    if (matcher.matches()) {
      return Long.valueOf(matcher.group(1));
    }

    log.warn("Destination not found: " + destination);

    return null;
  }
}
