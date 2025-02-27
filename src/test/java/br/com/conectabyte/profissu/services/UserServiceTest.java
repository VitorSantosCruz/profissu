package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.dtos.request.EmailValueRequestDto;
import br.com.conectabyte.profissu.dtos.request.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.SignUpConfirmationRequestDto;
import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.UserRepository;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;
import jakarta.mail.MessagingException;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class UserServiceTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TokenService tokenService;

  @Mock
  private EmailService emailService;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @Mock
  private RoleService roleService;

  @InjectMocks
  private UserService userService;

  @Test
  void shouldReturnUserWhenEmailIsValid() {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    final var optionalUser = this.userService.findByEmail(email);

    assertTrue(optionalUser.isPresent());
    assertTrue(optionalUser.get().getContacts().stream()
        .filter(c -> c.getValue().equals(email) && c.getType().equals(ContactTypeEnum.EMAIL) && c.isStandard()
            && c.getVerificationCompletedAt() != null)
        .findAny().isPresent());
  }

  @Test
  void shouldNotReturnUserWhenEmailIsInvalid() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    final var optionalUser = this.userService.findByEmail("invalid@conectabyte.com.br");

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldRegisterUserWhenUserDataIsValid() {
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(AddressUtils.create(user)));
    when(this.userRepository.save(any())).thenReturn(user);

    final var savedUser = this.userService.register(userMapper.userToUserRequestDto(user));

    assertTrue(savedUser != null);
  }

  @Test
  void shouldRecoverPasswordSucessfully() throws MessagingException {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    doNothing().when(this.tokenService).save(any(), any(), any());
    doNothing().when(this.emailService).sendPasswordRecoveryEmail(any(), any());

    this.userService.recoverPassword(new EmailValueRequestDto(email));

    verify(this.tokenService, times(1)).save(any(), any(), any());
    verify(this.emailService, times(1)).sendPasswordRecoveryEmail(any(), any());
  }

  @Test
  void shouldErrorWhenSendPasswordRecoveryEmailFail() throws MessagingException {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    doNothing().when(this.tokenService).save(any(), any(), any());
    doThrow(new MessagingException()).when(this.emailService).sendPasswordRecoveryEmail(any(), any());

    this.userService.recoverPassword(new EmailValueRequestDto(email));

    verify(this.tokenService, times(1)).save(any(), any(), any());
    verify(this.emailService, times(1)).sendPasswordRecoveryEmail(any(), any());
  }

  @Test
  void shouldErrorWhenUserWithThisEmailIsNotVerified() throws MessagingException {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of());

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    this.userService.recoverPassword(new EmailValueRequestDto(email));

    verify(this.tokenService, times(0)).save(any(), any(), any());
    verify(this.emailService, times(0)).sendPasswordRecoveryEmail(any(), any());
  }

  @Test
  void shouldErrorWhenUserWithInformedEmailNotExists() throws MessagingException {
    final var email = "test@conectabyte.com.br";

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.empty());
    this.userService.recoverPassword(new EmailValueRequestDto(email));

    verify(this.tokenService, times(0)).save(any(), any(), any());
    verify(this.emailService, times(0)).sendPasswordRecoveryEmail(any(), any());
  }

  @Test
  void shouldReturnBadRequestWhenUserNotFound() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    var response = userService.resetPassword(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("No user found with this e-mail.", response.message());
  }

  @Test
  void shouldReturnBadRequestWhenTokenIsNull() {
    final var user = UserUtils.create();
    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var response = userService.resetPassword(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("Missing reset code for user with this e-mail.", response.message());
  }

  @Test
  void shouldReturnBadRequestWhenTokenIsInvalid() {
    final var user = UserUtils.create();
    user.setToken(new Token());

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(false);

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var response = userService.resetPassword(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("Reset code is invalid.", response.message());
  }

  @Test
  void shouldReturnBadRequestWhenTokenIsExpired() {
    final var user = UserUtils.create();
    final var token = Token.builder()
        .value("CODE")
        .createdAt(LocalDateTime.now().minusMinutes(2))
        .build();
    user.setToken(token);

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);
    doNothing().when(tokenService).deleteByUser(any());

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var response = userService.resetPassword(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("Reset code is expired.", response.message());
  }

  @Test
  void shouldReturnOkWhenPasswordIsUpdated() {
    final var user = UserUtils.create();
    user.setToken(new Token());

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);
    when(bCryptPasswordEncoder.encode(any())).thenReturn("encoded");
    doNothing().when(tokenService).deleteByUser(any());

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var response = userService.resetPassword(requestDto);

    assertEquals(HttpStatus.OK.value(), response.responseCode());
    assertEquals("Password was updated.", response.message());
    verify(tokenService, times(1)).deleteByUser(user);
  }

  @Test
  void shouldResendSignUpConfirmationSucessfully() throws MessagingException {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    doNothing().when(this.tokenService).save(any(), any(), any());
    doNothing().when(this.emailService).sendSignUpConfirmation(any(), any());

    this.userService.resendSignUpConfirmation(new EmailValueRequestDto(email));

    verify(this.tokenService, times(1)).save(any(), any(), any());
    verify(this.emailService, times(1)).sendSignUpConfirmation(any(), any());
  }

  @Test
  void shouldErrorWhenUserWithThisEmailIsAlreadyVerified() throws MessagingException {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();

    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    this.userService.resendSignUpConfirmation(new EmailValueRequestDto(email));

    verify(this.tokenService, times(0)).save(any(), any(), any());
    verify(this.emailService, times(0)).sendPasswordRecoveryEmail(any(), any());
  }

  @Test
  void shouldReturnOkRequestWhenSignUpWasConfirmed() {
    final var user = UserUtils.create();
    final var token = Token.builder()
        .value("CODE")
        .createdAt(LocalDateTime.now())
        .build();

    user.setContacts(List.of(ContactUtils.create(user)));
    user.setToken(token);

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(this.userRepository.save(any())).thenReturn(user);
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);
    doNothing().when(tokenService).deleteByUser(any());

    final var requestDto = new SignUpConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var response = userService.signUpConfirmation(requestDto);

    assertEquals(HttpStatus.OK.value(), response.responseCode());
    assertEquals("Sign up was confirmed.", response.message());
  }

  @Test
  void shouldReturnBadRequestWhenNoUserFoundForInformedEmail() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    final var requestDto = new SignUpConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var response = userService.signUpConfirmation(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("No user found with this e-mail.", response.message());
  }

  @Test
  void shouldReturnBadRequestWhenMissingCodeForUserWithThisEmail() {
    final var user = UserUtils.create();

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

    final var requestDto = new SignUpConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var response = userService.signUpConfirmation(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("Missing reset code for user with this e-mail.", response.message());
  }

  @Test
  void shouldFindAnUserWhenUserWithIdExists() {
    when(userRepository.findById(any())).thenReturn(Optional.of(UserUtils.create()));

    final var user = userService.findById(any());

    assertNotNull(user);
  }

  @Test
  void shouldThrowsExceptionWhenUserNotBeFound() {
    when(userRepository.findById(any())).thenReturn(Optional.empty());

    final var exceptionMessage = assertThrows(ResourceNotFoundException.class, () -> userService.findById(any()));
    assertEquals("User not found.", exceptionMessage.getMessage());
  }
}
