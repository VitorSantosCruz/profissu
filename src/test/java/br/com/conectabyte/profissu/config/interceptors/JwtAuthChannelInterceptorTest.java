package br.com.conectabyte.profissu.config.interceptors;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import br.com.conectabyte.profissu.repositories.ConversationRepository;

@ExtendWith(MockitoExtension.class)
class JwtAuthChannelInterceptorTest {

  @Mock
  private JwtDecoder jwtDecoder;

  @Mock
  private ConversationRepository conversationRepository;

  @InjectMocks
  private JwtAuthChannelInterceptor interceptor;

  @Test
  void shouldAllowMessageWhenUserIsInConversation() {
    final var token = "Bearer TOKEN";
    final var claims = new HashMap<String, Object>();
    final var now = Instant.now();

    claims.put("sub", "1");

    final var jwt = new Jwt(token, now, now.plusSeconds(1), claims, claims);

    when(jwtDecoder.decode("TOKEN")).thenReturn(jwt);
    when(conversationRepository.isUserInConversation(1L, 1L)).thenReturn(true);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);

    accessor.setDestination("/topic/conversations/1/messages");
    accessor.setNativeHeader("token", token);

    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    final var result = interceptor.preSend(message, null);

    assertSame(message, result);
  }

  @Test
  void shouldRejectMessageWhenUserNotInConversation() {
    final var token = "Bearer TOKEN";
    final var claims = new HashMap<String, Object>();
    final var now = Instant.now();

    claims.put("sub", 1);

    final var jwt = new Jwt(token, now, now.plusSeconds(1), claims, claims);

    when(jwtDecoder.decode("TOKEN")).thenReturn(jwt);
    when(conversationRepository.isUserInConversation(1L, 1L)).thenReturn(false);

    final var accessor = StompHeaderAccessor.create(StompCommand.SEND);

    accessor.setDestination("/topic/conversations/1/messages");
    accessor.setNativeHeader("token", token);

    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    final var result = interceptor.preSend(message, null);

    assertNull(result);
  }

  @Test
  void shouldRejectMessageWhenTokenIsInvalid() {
    final var token = "Bearer TOKEN";

    when(jwtDecoder.decode("TOKEN")).thenThrow(new RuntimeException("Invalid token"));

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);

    accessor.setDestination("/topic/conversations/1/messages");
    accessor.setNativeHeader("token", token);

    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    final var result = interceptor.preSend(message, null);

    assertNull(result);
  }

  @Test
  void shouldRejectMessageWhenNoTokenProvided() {
    final var accessor = StompHeaderAccessor.create(StompCommand.SEND);

    accessor.setDestination("/topic/conversations/1/messages");

    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    final var result = interceptor.preSend(message, null);

    assertNull(result);
  }

  @Test
  void shouldRejectConnectIfTokenInvalid() {
    final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("token", " ");

    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    final var result = interceptor.preSend(message, null);

    assertNull(result);
  }

  @Test
  void shouldAllowConnectIfTokenValid() {
    final var token = "Bearer TOKEN";
    final var claims = new HashMap<String, Object>();
    final var now = Instant.now();

    claims.put("sub", "1");

    final var jwt = new Jwt(token, now, now.plusSeconds(1), claims, claims);

    when(jwtDecoder.decode(any())).thenReturn(jwt);

    final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("token", token);

    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    final var result = interceptor.preSend(message, null);

    assertSame(message, result);
  }

  @Test
  void shouldRejectMessageWhenDestinationIsInvalid() {
    final var token = "TOKEN";
    final var claims = new HashMap<String, Object>();
    final var now = Instant.now();

    claims.put("sub", "1");

    final var jwt = new Jwt(token, now, now.plusSeconds(1), claims, claims);

    when(jwtDecoder.decode("TOKEN")).thenReturn(jwt);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);

    accessor.setDestination("/invalid/destination");
    accessor.setNativeHeader("token", token);

    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    final var result = interceptor.preSend(message, null);

    assertNull(result);
  }
}
