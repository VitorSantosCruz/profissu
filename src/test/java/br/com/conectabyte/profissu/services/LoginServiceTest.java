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
  private TokenService tokenService;

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
    final var password = "$2y$10$pZKpygPyYuXXySPufr4VAeNrcKhxueFwXXNm.p7mvrKnUSamaXoPy";
    final var user = UserUtils.createUser();
    final var contact = ContactUtils.createContact(user);
    user.setContacts(List.of(contact));

    when(tokenService.create(any())).thenReturn(new LoginResponseDto(token, expiresIn));
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
    final var password = "$2y$10$pZKpygPyYuXXySPufr4VAeNrcKhxueFwXXNm.p7mvrKnUSamaXoPy";

    when(tokenService.create(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    assertThrows(BadCredentialsException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }

  @Test
  void shouldThrowExceptionWhenPasswordIsInvalid() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$pZKpygPyYuXXySPufr4VAeNrcKhxueFwXXNm.p7mvrKnUSamaXoPy";

    when(tokenService.create(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(Optional.of(UserUtils.createUser()));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    assertThrows(BadCredentialsException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }

  @Test
  void shouldThrowExceptionWhenEmailIsUnverified() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$pZKpygPyYuXXySPufr4VAeNrcKhxueFwXXNm.p7mvrKnUSamaXoPy";
    final var user = UserUtils.createUser();
    final var contact = ContactUtils.createContact(user);
    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(tokenService.create(any())).thenReturn(new LoginResponseDto(token, expiresIn));
    when(userService.findByEmail(any())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    assertThrows(EmailNotVerifiedException.class, () -> loginService.login(new LoginRequestDto(email, password)));
  }
}
