package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.repositories.TokenRepository;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  @Mock
  private TokenRepository tokenRepository;

  @Mock
  private Token token;

  @Mock
  private User user;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @InjectMocks
  private TokenService tokenService;

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
    when(token.getUser()).thenReturn(user);
    doNothing().when(tokenRepository).delete(any());

    tokenService.deleteByUser(user);

    verify(tokenRepository, times(1)).delete(token);
    verify(user, times(1)).setToken(null);
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

}
