package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.dtos.request.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {
  @Mock
  private JwtService jwtService;

  @Mock
  private UserService userService;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @InjectMocks
  private LoginService loginService;

  @Test
  void shouldReturnTokenWhenCredentialsAreValid() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var user = UserUtils.create();
    final var contact = ContactUtils.createEmail(user);
    user.setContacts(List.of(contact));

    when(jwtService.createJwtToken(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(user);
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);

    final var loginResponseDto = loginService.login(new LoginRequestDto(email, password));

    assertTrue(loginResponseDto.accessToken().equals(token));
    assertTrue(loginResponseDto.expiresIn().equals(expiresIn));
  }

  @Test
  void shouldThrowExceptionWhenUserNotFoundWithEmail() {
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";

    when(userService.findByEmail(any())).thenThrow(ResourceNotFoundException.class);

    assertThrows(BadCredentialsException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }

  @Test
  void shouldThrowExceptionWhenPasswordIsInvalid() {
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";

    when(userService.findByEmail(any())).thenReturn(UserUtils.create());

    assertThrows(BadCredentialsException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }

  @Test
  void shouldThrowExceptionWhenEmailIsUnverified() {
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var user = UserUtils.create();
    final var contact = ContactUtils.createEmail(user);
    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(userService.findByEmail(any())).thenReturn(user);
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);

    assertThrows(EmailNotVerifiedException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }
}
