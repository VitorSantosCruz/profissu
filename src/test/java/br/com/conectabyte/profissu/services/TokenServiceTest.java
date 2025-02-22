package br.com.conectabyte.profissu.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.repositories.TokenRepository;

@SpringBootTest
@ActiveProfiles("test")
class TokenServiceTest {

  @Mock
  private TokenRepository tokenRepository;

  @Mock
  private Token token;

  @Mock
  private User user;

  @InjectMocks
  private TokenService tokenService;

  @Test
  void shouldSaveTokenSuccessfully() {
    when(tokenRepository.save(any(Token.class))).thenReturn(token);

    tokenService.save(token);

    verify(tokenRepository, times(1)).save(token);
  }

  @Test
  void shouldDeleteTokenSuccessfully() {
    doNothing().when(tokenRepository).delete(any(Token.class));

    tokenService.delete(token);

    verify(tokenRepository, times(1)).delete(token);
  }

  @Test
  void shouldDeleteTokenByUserSuccessfully() {
    when(user.getToken()).thenReturn(token);
    when(token.getUser()).thenReturn(user);
    doNothing().when(tokenRepository).delete(any(Token.class));

    tokenService.deleteByUser(user);

    verify(tokenRepository, times(1)).delete(token);
    verify(user, times(1)).setToken(null);
  }

  @Test
  void shouldNotDeleteTokenWhenUserHasNoToken() {
    when(user.getToken()).thenReturn(null);

    tokenService.deleteByUser(user);

    verify(tokenRepository, times(0)).delete(any(Token.class));
    verify(user, times(0)).setToken(null);
  }
}
