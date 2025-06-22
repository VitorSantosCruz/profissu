package br.com.conectabyte.profissu.config.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
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

import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.repositories.ConversationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthChannelInterceptor Tests")
class JwtAuthChannelInterceptorTest {

  @Mock
  private JwtDecoder jwtDecoder;

  @Mock
  private ConversationRepository conversationRepository;

  @InjectMocks
  private JwtAuthChannelInterceptor interceptor;

  private static final String VALID_TOKEN_VALUE = "VALID_TOKEN_STRING";
  private static final String INVALID_TOKEN_VALUE = "INVALID_TOKEN_STRING";
  private static final String TOKEN_WITH_BEARER = "Bearer " + VALID_TOKEN_VALUE;
  private static final String TOKEN_WITHOUT_SUB_CLAIM = "TOKEN_WITHOUT_SUB";
  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_CONVERSATION_ID = 1L;
  private static final String VALID_DESTINATION = "/topic/conversations/" + TEST_CONVERSATION_ID + "/messages";
  private static final String INVALID_DESTINATION = "/invalid/destination";

  @Test
  @DisplayName("Deve permitir a mensagem quando o usuário está na conversa (comando SUBSCRIBE)")
  void shouldAllowMessageWhenUserIsInConversationSubscribe() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", TEST_USER_ID.toString());
    Jwt jwt = new Jwt(VALID_TOKEN_VALUE, Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);

    when(jwtDecoder.decode(VALID_TOKEN_VALUE)).thenReturn(jwt);
    when(conversationRepository.isUserInConversation(TEST_USER_ID, TEST_CONVERSATION_ID)).thenReturn(true);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(VALID_DESTINATION);
    accessor.setNativeHeader("token", TOKEN_WITH_BEARER);
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertSame(message, result);
    verify(jwtDecoder, times(1)).decode(VALID_TOKEN_VALUE);
    verify(conversationRepository, times(1)).isUserInConversation(TEST_USER_ID, TEST_CONVERSATION_ID);
  }

  @Test
  @DisplayName("Deve permitir a mensagem quando o usuário está na conversa (comando SEND)")
  void shouldAllowMessageWhenUserIsInConversationSend() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", TEST_USER_ID.toString());
    Jwt jwt = new Jwt(VALID_TOKEN_VALUE, Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);

    when(jwtDecoder.decode(VALID_TOKEN_VALUE)).thenReturn(jwt);
    when(conversationRepository.isUserInConversation(TEST_USER_ID, TEST_CONVERSATION_ID)).thenReturn(true);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination(VALID_DESTINATION);
    accessor.setNativeHeader("token", TOKEN_WITH_BEARER);
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertSame(message, result);
    verify(jwtDecoder, times(1)).decode(VALID_TOKEN_VALUE);
    verify(conversationRepository, times(1)).isUserInConversation(TEST_USER_ID, TEST_CONVERSATION_ID);
  }

  @Test
  @DisplayName("Deve rejeitar a mensagem quando o usuário não está na conversa")
  void shouldRejectMessageWhenUserNotInConversation() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", TEST_USER_ID.toString());
    Jwt jwt = new Jwt(VALID_TOKEN_VALUE, Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);

    when(jwtDecoder.decode(VALID_TOKEN_VALUE)).thenReturn(jwt);
    when(conversationRepository.isUserInConversation(TEST_USER_ID, TEST_CONVERSATION_ID)).thenReturn(false);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(VALID_DESTINATION);
    accessor.setNativeHeader("token", TOKEN_WITH_BEARER);
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertNull(result);
    verify(jwtDecoder, times(1)).decode(VALID_TOKEN_VALUE);
    verify(conversationRepository, times(1)).isUserInConversation(TEST_USER_ID, TEST_CONVERSATION_ID);
  }

  @Test
  @DisplayName("Deve rejeitar a mensagem quando o token JWT é inválido (durante a decodificação)")
  void shouldRejectMessageWhenTokenIsInvalidDecoding() {
    when(jwtDecoder.decode(INVALID_TOKEN_VALUE)).thenThrow(new RuntimeException("Token inválido"));

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(VALID_DESTINATION);
    accessor.setNativeHeader("token", "Bearer " + INVALID_TOKEN_VALUE);
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertNull(result);
    verify(jwtDecoder, times(1)).decode(INVALID_TOKEN_VALUE);
    verify(conversationRepository, never()).isUserInConversation(anyLong(), anyLong());
  }

  @Test
  @DisplayName("Deve rejeitar a mensagem quando nenhum token é fornecido")
  void shouldRejectMessageWhenNoTokenProvided() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(VALID_DESTINATION);
    // Sem nativeHeader("token")
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertNull(result);
    verify(jwtDecoder, never()).decode(anyString());
    verify(conversationRepository, never()).isUserInConversation(anyLong(), anyLong());
  }

  @Test
  @DisplayName("Deve rejeitar a mensagem quando o token é em branco")
  void shouldRejectMessageWhenTokenIsBlank() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(VALID_DESTINATION);
    accessor.setNativeHeader("token", " "); // Token em branco
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertNull(result);
    verify(jwtDecoder, never()).decode(anyString());
    verify(conversationRepository, never()).isUserInConversation(anyLong(), anyLong());
  }

  @Test
  @DisplayName("Deve permitir comandos que não sejam SUBSCRIBE ou SEND sem validação de conversa")
  void shouldAllowNonSubscribeOrSendCommands() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", TEST_USER_ID.toString());
    Jwt jwt = new Jwt(VALID_TOKEN_VALUE, Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);

    when(jwtDecoder.decode(VALID_TOKEN_VALUE)).thenReturn(jwt);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT); // Comando CONNECT
    accessor.setNativeHeader("token", TOKEN_WITH_BEARER);
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertSame(message, result);
    verify(jwtDecoder, times(1)).decode(VALID_TOKEN_VALUE);
    verify(conversationRepository, never()).isUserInConversation(anyLong(), anyLong()); // Não deve validar conversa
  }

  @Test
  @DisplayName("Deve rejeitar a mensagem quando o ID de conversação não pode ser extraído do destino")
  void shouldRejectMessageWhenConversationIdCannotBeExtracted() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", TEST_USER_ID.toString());
    Jwt jwt = new Jwt(VALID_TOKEN_VALUE, Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);

    when(jwtDecoder.decode(VALID_TOKEN_VALUE)).thenReturn(jwt);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(INVALID_DESTINATION); // Destino inválido
    accessor.setNativeHeader("token", TOKEN_WITH_BEARER);
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    org.springframework.messaging.Message<?> result = interceptor.preSend(message, null);

    assertNull(result);
    verify(jwtDecoder, times(1)).decode(VALID_TOKEN_VALUE);
    verify(conversationRepository, never()).isUserInConversation(anyLong(), anyLong());
  }

  @Test
  @DisplayName("Deve rejeitar a mensagem quando a claim 'sub' (ID do usuário) está faltando no JWT")
  void shouldRejectMessageWhenUserIdMissingInClaims() {
    Map<String, Object> claimsWithoutSub = new HashMap<>();
    claimsWithoutSub.put("role", "USER"); // Claim sem 'sub'
    Jwt jwtWithoutSub = new Jwt(TOKEN_WITHOUT_SUB_CLAIM, Instant.now(), Instant.now().plusSeconds(3600),
        Map.of("alg", "none"), claimsWithoutSub);

    when(jwtDecoder.decode(TOKEN_WITHOUT_SUB_CLAIM)).thenReturn(jwtWithoutSub);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(VALID_DESTINATION);
    accessor.setNativeHeader("token", "Bearer " + TOKEN_WITHOUT_SUB_CLAIM);
    org.springframework.messaging.Message<?> message = MessageBuilder.createMessage(new byte[0],
        accessor.getMessageHeaders());

    final var exception = assertThrows(ValidationException.class, () -> {
      interceptor.preSend(message, null);
    });

    assertEquals("User ID not found in JWT claims.", exception.getMessage());
  }
}
