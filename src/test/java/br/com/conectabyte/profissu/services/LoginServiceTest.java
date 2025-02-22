package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@SpringBootTest
@ActiveProfiles("test")
public class LoginServiceTest {
  @MockBean
  private JwtService jwtService;

  @MockBean
  private UserService userService;

  @MockBean
  private BCryptPasswordEncoder passwordEncoder;

  @Autowired
  private LoginService loginService;

  @Test
  void shouldReturnTokenWhenCredentialsAreValid() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(jwtService.createJwtToken(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    final var loginResponseDto = loginService.login(new LoginRequestDto(email, password));

    assertTrue(loginResponseDto.accessToken().equals(token));
    assertTrue(loginResponseDto.expiresIn().equals(expiresIn));
  }

  @Test
  void shouldThrowExceptionWhenUserNotFoundWithEmail() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";

    when(jwtService.createJwtToken(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    assertThrows(BadCredentialsException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }

  @Test
  void shouldThrowExceptionWhenPasswordIsInvalid() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";

    when(jwtService.createJwtToken(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(Optional.of(UserUtils.create()));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    assertThrows(BadCredentialsException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }

  @Test
  void shouldThrowExceptionWhenEmailIsUnverified() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(jwtService.createJwtToken(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    assertThrows(EmailNotVerifiedException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }
}
