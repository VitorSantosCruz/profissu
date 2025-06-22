package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.dtos.request.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginService Tests")
public class LoginServiceTest {
  @Mock
  private JwtService jwtService;

  @Mock
  private UserService userService;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @InjectMocks
  private LoginService loginService;

  private static final String TEST_EMAIL = "test@conectabyte.com.br";
  private static final String TEST_PASSWORD = "rawPassword";

  @Test
  @DisplayName("Should return token when credentials are valid and email is verified")
  void shouldReturnTokenWhenCredentialsAreValid() {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(user);
    when(bCryptPasswordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(true);
    when(jwtService.createJwtToken(user)).thenReturn(new LoginResponseDto(token, expiresIn));

    final var loginResponseDto = loginService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD));

    assertTrue(loginResponseDto.accessToken().equals(token));
    assertTrue(loginResponseDto.expiresIn().equals(expiresIn));
    verify(userService).findByEmail(TEST_EMAIL);
    verify(bCryptPasswordEncoder).matches(TEST_PASSWORD, user.getPassword());
    verify(jwtService).createJwtToken(user);
  }

  @Test
  @DisplayName("Should throw BadCredentialsException when user not found with email")
  void shouldThrowExceptionWhenUserNotFoundWithEmail() {
    when(userService.findByEmail(TEST_EMAIL)).thenThrow(ResourceNotFoundException.class);

    assertThrows(BadCredentialsException.class,
        () -> loginService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD)));
    verify(userService).findByEmail(TEST_EMAIL);
    verify(bCryptPasswordEncoder, org.mockito.Mockito.never()).matches(anyString(), anyString());
    verify(jwtService, org.mockito.Mockito.never()).createJwtToken(any(User.class));
  }

  @Test
  @DisplayName("Should throw BadCredentialsException when password is invalid")
  void shouldThrowExceptionWhenPasswordIsInvalid() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(user);
    when(bCryptPasswordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(false);
    assertThrows(BadCredentialsException.class,
        () -> loginService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD)));
    verify(userService).findByEmail(TEST_EMAIL);
    verify(bCryptPasswordEncoder).matches(TEST_PASSWORD, user.getPassword());
    verify(jwtService, org.mockito.Mockito.never()).createJwtToken(any(User.class));
  }

  @Test
  @DisplayName("Should throw EmailNotVerifiedException when email is unverified")
  void shouldThrowExceptionWhenEmailIsUnverified() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(userService.findByEmail(TEST_EMAIL)).thenReturn(user);
    when(bCryptPasswordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(true);
    assertThrows(EmailNotVerifiedException.class,
        () -> loginService.login(new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD)));
    verify(userService).findByEmail(TEST_EMAIL);
    verify(bCryptPasswordEncoder).matches(TEST_PASSWORD, user.getPassword());
    verify(jwtService, org.mockito.Mockito.never()).createJwtToken(any(User.class));
  }
}
