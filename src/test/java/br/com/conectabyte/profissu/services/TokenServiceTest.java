package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.repositories.TokenRepository;
import br.com.conectabyte.profissu.utils.PropertiesLoader;
import br.com.conectabyte.profissu.utils.TokenUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
  @Mock
  private TokenRepository tokenRepository;

  @Mock
  private User user;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @Mock
  private ProfissuProperties profissuProperties;

  @InjectMocks
  private TokenService tokenService;

  private final Token token = TokenUtils.create(UserUtils.create());

  void setUp() throws Exception {
    final var loadedProfissuProperties = new PropertiesLoader().loadProperties();

    when(profissuProperties.getProfissu()).thenReturn(loadedProfissuProperties.getProfissu());
  }

  @Test
  void shouldSaveTokenSuccessfully() {
    when(tokenRepository.save(any())).thenReturn(token);

    tokenService.save(token);

    verify(tokenRepository, times(1)).save(token);
  }

  @Test
  void shouldDeleteTokenSuccessfully() {
    doNothing().when(tokenRepository).delete(any());

    tokenService.delete(token);

    verify(tokenRepository, times(1)).delete(token);
  }

  @Test
  void shouldDeleteTokenByUserSuccessfully() {
    when(user.getToken()).thenReturn(token);
    doNothing().when(tokenRepository).delete(any());

    tokenService.deleteByUser(user);

    verify(tokenRepository, times(1)).delete(token);
  }

  @Test
  void shouldNotDeleteTokenWhenUserHasNoToken() {
    when(user.getToken()).thenReturn(null);

    tokenService.deleteByUser(user);

    verify(tokenRepository, times(0)).delete(any());
    verify(user, times(0)).setToken(null);
  }

  @Test
  void shouldBuildAndSaveTokenSuccessfully() {
    final var user = UserUtils.create();
    final var encodedValue = "encoded";
    final var tokenCaptor = ArgumentCaptor.forClass(Token.class);

    when(tokenRepository.save(any())).thenReturn(token);
    when(bCryptPasswordEncoder.encode(any())).thenReturn(encodedValue);

    tokenService.save(user, "CODE", bCryptPasswordEncoder);

    verify(tokenRepository).save(tokenCaptor.capture());

    final var savedToken = tokenCaptor.getValue();

    assertEquals(encodedValue, savedToken.getValue());
    assertEquals(user, savedToken.getUser());
  }

  @Test
  void shouldReturnErrorWhenTokenIsMissing() {
    when(user.getToken()).thenReturn(null);

    final var result = tokenService.validateToken(user, "test@example.com", "code");

    assertEquals("Missing reset code for user with this e-mail.", result);
  }

  @Test
  void shouldReturnErrorWhenTokenIsInvalid() {
    when(user.getToken()).thenReturn(token);
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(false);

    final var result = tokenService.validateToken(user, "test@example.com", "invalidCode");

    assertEquals("Reset code is invalid.", result);
  }

  @Test
  void shouldReturnErrorWhenTokenIsExpired() throws Exception {
    setUp();
    token.setCreatedAt(LocalDateTime.now().minusMinutes(2));
    when(user.getToken()).thenReturn(token);
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);

    final var result = tokenService.validateToken(user, "test@example.com", "validCode");

    assertEquals("Reset code is expired.", result);
  }

  @Test
  void shouldReturnNullWhenTokenIsValid() throws Exception {
    setUp();
    token.setCreatedAt(LocalDateTime.now().plusMinutes(2));
    when(user.getToken()).thenReturn(token);
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);

    final var result = tokenService.validateToken(user, "test@example.com", "validCode");

    assertEquals(null, result);
  }

  @Test
  void shouldInvokeFlushOnRepository() {
    doNothing().when(tokenRepository).flush();

    tokenService.flush();

    verify(tokenRepository, times(1)).flush();
  }
}
