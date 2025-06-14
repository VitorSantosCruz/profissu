package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.dtos.request.EmailValueRequestDto;
import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.ProfileRequestDto;
import br.com.conectabyte.profissu.dtos.request.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.enums.GenderEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.UserRepository;
import br.com.conectabyte.profissu.services.email.PasswordRecoveryEmailService;
import br.com.conectabyte.profissu.services.email.SignUpConfirmationService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;
import jakarta.mail.MessagingException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TokenService tokenService;

  @Mock
  private PasswordRecoveryEmailService passwordRecoveryEmailService;

  @Mock
  private SignUpConfirmationService signUpConfirmationService;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @Mock
  private RoleService roleService;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private UserService userService;

  @Test
  void shouldReturnUserWhenEmailIsValid() {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    final var findedUser = this.userService.findByEmail(email);

    assertTrue(findedUser.getContacts().stream()
        .filter(Contact::isStandard)
        .filter(c -> c.getVerificationCompletedAt() != null)
        .filter(c -> c.getValue().equals(email))
        .findAny().isPresent());
  }

  @Test
  void shouldNotReturnUserWhenEmailIsInvalid() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    final var exceptionMessage = assertThrows(ResourceNotFoundException.class,
        () -> this.userService.findByEmail(any()));

    assertEquals("User not found.", exceptionMessage.getMessage());
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
    doNothing().when(this.passwordRecoveryEmailService).send(any());
    doNothing().when(tokenService).flush();

    this.userService.recoverPassword(new EmailValueRequestDto(email));

    verify(this.tokenService, times(1)).save(any(), any(), any());
    verify(this.passwordRecoveryEmailService, times(1)).send(any());
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
    verify(this.passwordRecoveryEmailService, times(0)).send(any());
  }

  @Test
  void shouldErrorWhenUserWithInformedEmailNotExists() throws MessagingException {
    final var email = "test@conectabyte.com.br";

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.empty());
    this.userService.recoverPassword(new EmailValueRequestDto(email));

    verify(this.tokenService, times(0)).save(any(), any(), any());
    verify(this.passwordRecoveryEmailService, times(0)).send(any());
  }

  @Test
  void shouldReturnBadRequestWhenUserNotFound() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var exception = assertThrows(ValidationException.class, () -> userService.resetPassword(requestDto));

    assertEquals("No user found with this e-mail.", exception.getMessage());
  }

  @Test
  void shouldReturnBadRequestWhenTokenIsNull() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(tokenService.validateToken(any(), any(), any())).thenReturn("Missing reset code for user with this e-mail.");

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var exception = assertThrows(ValidationException.class, () -> userService.resetPassword(requestDto));

    assertEquals("Missing reset code for user with this e-mail.", exception.getMessage());
  }

  @Test
  void shouldReturnBadRequestWhenTokenIsInvalid() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(tokenService.validateToken(any(), any(), any())).thenReturn("Reset code is invalid.");

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var exception = assertThrows(ValidationException.class, () -> userService.resetPassword(requestDto));

    assertEquals("Reset code is invalid.", exception.getMessage());
  }

  @Test
  void shouldReturnBadRequestWhenTokenIsExpired() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(tokenService.validateToken(any(), any(), any())).thenReturn("Reset code is expired.");

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var exception = assertThrows(ValidationException.class, () -> userService.resetPassword(requestDto));

    assertEquals("Reset code is expired.", exception.getMessage());
  }

  @Test
  void shouldReturnOkWhenPasswordIsUpdated() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));
    user.setToken(new Token());

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(bCryptPasswordEncoder.encode(any())).thenReturn("encoded");
    doNothing().when(tokenService).deleteByUser(any());

    final var requestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "admin", "CODE");
    final var response = userService.resetPassword(requestDto);

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
    doNothing().when(this.signUpConfirmationService).send(any());
    doNothing().when(tokenService).flush();

    this.userService.resendSignUpConfirmation(new EmailValueRequestDto(email));

    verify(this.tokenService, times(1)).save(any(), any(), any());
    verify(this.signUpConfirmationService, times(1)).send(any());
  }

  @Test
  void shouldErrorWhenUserWithThisEmailIsAlreadyVerified() throws MessagingException {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();

    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    this.userService.resendSignUpConfirmation(new EmailValueRequestDto(email));

    verify(this.tokenService, times(0)).save(any(), any(), any());
    verify(this.passwordRecoveryEmailService, times(0)).send(any());
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

  @Test
  void shouldMarkUserAsDeleted() {
    final var user = UserUtils.create();
    when(userRepository.findById(any())).thenReturn(Optional.of(user));

    userService.deleteById(any());

    assertNotNull(user.getDeletedAt());
  }

  @Test
  void shouldUpdatePasswordWhenDataIsValid() {
    final var user = UserUtils.create();

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userRepository.findById(any())).thenReturn(Optional.of(user));
    when(this.userRepository.save(any())).thenReturn(user);
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(true);
    when(bCryptPasswordEncoder.encode(any())).thenReturn("encoded");

    userService.updatePassword(new PasswordRequestDto("currentPassword", "newPassword"));

    verify(userRepository, times(1)).save(user);
  }

  @Test
  void shouldThrowsExceptionWhenUserNotFound() {
    final var passwordRequestDto = new PasswordRequestDto("currentPassword", "newPassword");

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userRepository.findById(any())).thenReturn(Optional.empty());

    final var exceptionMessage = assertThrows(ResourceNotFoundException.class,
        () -> userService.updatePassword(passwordRequestDto));

    assertEquals("User not found.", exceptionMessage.getMessage());

    verify(userRepository, times(0)).save(any());
    verify(bCryptPasswordEncoder, times(0)).matches(any(CharSequence.class), any(String.class));
    verify(bCryptPasswordEncoder, times(0)).encode(any());
  }

  @Test
  void shouldThrowsExceptionWhenCurrentPasswordNotMatches() {
    final var passwordRequestDto = new PasswordRequestDto("currentPassword", "newPassword");

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userRepository.findById(any())).thenReturn(Optional.of(UserUtils.create()));
    when(bCryptPasswordEncoder.matches(any(), any())).thenReturn(false);

    final var exceptionMessage = assertThrows(BadCredentialsException.class,
        () -> userService.updatePassword(passwordRequestDto));

    assertEquals("Current password is not valid.", exceptionMessage.getMessage());

    verify(userRepository, times(0)).save(any());
    verify(bCryptPasswordEncoder, times(0)).encode(any());
  }

  @Test
  void shouldReturnUserResponseDtoWhenUserExists() {
    final var user = UserUtils.create();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    final var result = userService.findByIdAndReturnDto(1L);

    assertEquals(userMapper.userToUserResponseDto(user), result);
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenUserNotExists() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
      userService.findByIdAndReturnDto(1L);
    });

    assertEquals("User not found.", exception.getMessage());
  }

  @Test
  void shouldUpdateUserProfileWhenUserExists() {
    final var user = UserUtils.create();

    final var newName = "New Name";
    final var newBio = "New Bio";
    final var newGender = GenderEnum.FEMALE;
    final var profileRequestDto = new ProfileRequestDto(newName, newBio, newGender);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    final var result = userService.update(profileRequestDto);

    assertEquals(userMapper.userToUserResponseDto(user), result);

    verify(userRepository).save(user);
  }

  @Test
  void shouldBeThrowResourceNotFoundExceptionWhenUserNotExists() {
    final var newName = "New Name";
    final var newBio = "New Bio";
    final var newGender = GenderEnum.FEMALE;
    final var profileRequestDto = new ProfileRequestDto(newName, newBio, newGender);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    final var exception = assertThrows(ResourceNotFoundException.class, () -> {
      userService.update(profileRequestDto);
    });

    assertEquals("User not found.", exception.getMessage());
  }
}
