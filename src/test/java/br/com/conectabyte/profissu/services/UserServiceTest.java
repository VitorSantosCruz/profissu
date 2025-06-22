package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.dtos.request.EmailCodeDto;
import br.com.conectabyte.profissu.dtos.request.EmailValueRequestDto;
import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.ProfileRequestDto;
import br.com.conectabyte.profissu.dtos.request.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.UserRequestDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.GenderEnum;
import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.UserRepository;
import br.com.conectabyte.profissu.services.email.PasswordRecoveryEmailService;
import br.com.conectabyte.profissu.services.email.SignUpConfirmationService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.RoleUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
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

  private static final Long TEST_USER_ID = 1L;
  private static final String TEST_EMAIL = "test@conectabyte.com.br";
  private static final String TEST_PASSWORD = "password123";
  private static final String ENCODED_PASSWORD = "$2a$10$encodedpasswordhash";
  private static final String VALID_CODE = "123456";
  private static final String INVALID_CODE = "invalidCode";

  @Test
  @DisplayName("Should return user when email is valid and found")
  void shouldReturnUserWhenEmailIsValid() {
    User user = UserUtils.create();
    Contact contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(this.userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));
    User foundUser = this.userService.findByEmail(TEST_EMAIL);

    assertNotNull(foundUser);
    assertEquals(user.getId(), foundUser.getId());
    verify(this.userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user email is invalid or not found")
  void shouldNotReturnUserWhenEmailIsInvalid() {
    when(userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> this.userService.findByEmail(TEST_EMAIL));

    assertEquals("User not found.", exception.getMessage());
    verify(this.userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
  }

  @Test
  @DisplayName("Should register user successfully when user data is valid")
  void shouldRegisterUserWhenUserDataIsValid() {
    final var userToRegister = UserUtils.create();
    userToRegister.setId(null);
    userToRegister.setPassword(TEST_PASSWORD);
    final var contactToRegister = ContactUtils.create(userToRegister);
    final var addressToRegister = AddressUtils.create(userToRegister);
    userToRegister.setContacts(List.of(contactToRegister));
    userToRegister.setAddresses(List.of(addressToRegister));
    userToRegister.setGender(GenderEnum.MALE);
    userToRegister.setBio("Some bio");

    User registeredUser = UserUtils.create();
    registeredUser.setId(TEST_USER_ID);
    registeredUser.setName(userToRegister.getName());
    registeredUser.setContacts(List.of(contactToRegister));
    registeredUser.setGender(GenderEnum.MALE);
    registeredUser.setBio("Some bio");

    UserRequestDto userRequestDto = userMapper.userToUserRequestDto(userToRegister);

    when(roleService.findByName(eq(RoleEnum.USER.name())))
        .thenReturn(Optional.of(RoleUtils.create(RoleEnum.USER.name())));
    when(bCryptPasswordEncoder.encode(eq(TEST_PASSWORD))).thenReturn(ENCODED_PASSWORD);
    when(userRepository.save(any(User.class))).thenReturn(registeredUser);
    doNothing().when(tokenService).save(any(User.class), anyString(), any(BCryptPasswordEncoder.class));
    doNothing().when(signUpConfirmationService).send(any(EmailCodeDto.class));

    final var savedUserResponse = this.userService.register(userRequestDto);

    assertNotNull(savedUserResponse);
    assertEquals(TEST_USER_ID, savedUserResponse.id());
    assertEquals(userToRegister.getName(), savedUserResponse.name());
    assertEquals(userToRegister.getBio(), savedUserResponse.bio());
    assertEquals(userToRegister.getContacts().get(0).getValue(), savedUserResponse.contacts().get(0).value());
    verify(userRepository, times(1)).save(any(User.class));
    verify(tokenService, times(1)).save(eq(registeredUser), anyString(), eq(bCryptPasswordEncoder));
    verify(signUpConfirmationService, times(1)).send(any(EmailCodeDto.class));
  }

  @Test
  @DisplayName("Should recover password successfully")
  void shouldRecoverPasswordSucessfully() {
    User user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));
    doNothing().when(this.tokenService).save(any(User.class), anyString(), any(BCryptPasswordEncoder.class));
    doNothing().when(this.passwordRecoveryEmailService).send(any());
    doNothing().when(tokenService).flush();
    doNothing().when(tokenService).deleteByUser(any(User.class));

    this.userService.recoverPassword(new EmailValueRequestDto(TEST_EMAIL));

    verify(this.tokenService, times(1)).deleteByUser(eq(user));
    verify(this.tokenService, times(1)).flush();
    verify(this.tokenService, times(1)).save(eq(user), anyString(), any(BCryptPasswordEncoder.class));
    verify(this.passwordRecoveryEmailService, times(1)).send(any());
  }

  @Test
  @DisplayName("Should not send password recovery email when user with this email is not verified")
  void shouldErrorWhenUserWithThisEmailIsNotVerified() {
    User user = UserUtils.create();
    Contact contact = ContactUtils.create(user);
    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(this.userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));

    this.userService.recoverPassword(new EmailValueRequestDto(TEST_EMAIL));

    verify(this.userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(this.tokenService, never()).save(any(), any(), any());
    verify(this.passwordRecoveryEmailService, never()).send(any());
    verify(this.tokenService, never()).deleteByUser(any());
    verify(this.tokenService, never()).flush();
  }

  @Test
  @DisplayName("Should not send password recovery email when user with informed email does not exist")
  void shouldErrorWhenUserWithInformedEmailNotExists() {
    when(this.userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.empty());

    this.userService.recoverPassword(new EmailValueRequestDto(TEST_EMAIL));

    verify(this.userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(this.tokenService, never()).save(any(), any(), any());
    verify(this.passwordRecoveryEmailService, never()).send(any());
    verify(this.tokenService, never()).deleteByUser(any());
    verify(this.tokenService, never()).flush();
  }

  @Test
  @DisplayName("Should throw ValidationException when user not found during password reset")
  void shouldReturnBadRequestWhenUserNotFound() {
    when(userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.empty());

    ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto(TEST_EMAIL, TEST_PASSWORD, VALID_CODE);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userService.resetPassword(requestDto));

    assertEquals("No user found with this e-mail.", exception.getMessage());
    verify(userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(tokenService, never()).validateToken(any(), any(), any());
  }

  @Test
  @DisplayName("Should throw ValidationException when token validation fails (missing)")
  void shouldReturnBadRequestWhenTokenIsNull() {
    User user = UserUtils.create();
    Contact contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));
    when(tokenService.validateToken(eq(user), eq(TEST_EMAIL), eq(VALID_CODE)))
        .thenReturn("Missing reset code for user with this e-mail.");

    ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto(TEST_EMAIL, TEST_PASSWORD, VALID_CODE);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userService.resetPassword(requestDto));

    assertEquals("Missing reset code for user with this e-mail.", exception.getMessage());
    verify(userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(tokenService, times(1)).validateToken(eq(user), eq(TEST_EMAIL), eq(VALID_CODE));
  }

  @Test
  @DisplayName("Should throw ValidationException when token validation fails (invalid)")
  void shouldReturnBadRequestWhenTokenIsInvalid() {
    User user = UserUtils.create();
    Contact contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));
    when(tokenService.validateToken(eq(user), eq(TEST_EMAIL), eq(INVALID_CODE))).thenReturn("Reset code is invalid.");

    ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto(TEST_EMAIL, TEST_PASSWORD, INVALID_CODE);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userService.resetPassword(requestDto));

    assertEquals("Reset code is invalid.", exception.getMessage());
    verify(userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(tokenService, times(1)).validateToken(eq(user), eq(TEST_EMAIL), eq(INVALID_CODE));
  }

  @Test
  @DisplayName("Should throw ValidationException when token validation fails (expired)")
  void shouldReturnBadRequestWhenTokenIsExpired() {
    User user = UserUtils.create();
    Contact contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));
    when(tokenService.validateToken(eq(user), eq(TEST_EMAIL), eq(VALID_CODE))).thenReturn("Reset code is expired.");

    ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto(TEST_EMAIL, TEST_PASSWORD, VALID_CODE);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userService.resetPassword(requestDto));

    assertEquals("Reset code is expired.", exception.getMessage());
    verify(userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(tokenService, times(1)).validateToken(eq(user), eq(TEST_EMAIL), eq(VALID_CODE));
  }

  @Test
  @DisplayName("Should resend sign up confirmation successfully when user is unverified")
  void shouldResendSignUpConfirmationSucessfully() {
    User user = UserUtils.create();
    Contact contact = ContactUtils.create(user);
    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(this.userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));
    doNothing().when(this.tokenService).deleteByUser(eq(user));
    doNothing().when(this.tokenService).flush();
    doNothing().when(this.tokenService).save(any(User.class), anyString(), any(BCryptPasswordEncoder.class));
    doNothing().when(this.signUpConfirmationService).send(any(EmailCodeDto.class));

    this.userService.resendSignUpConfirmation(new EmailValueRequestDto(TEST_EMAIL));

    verify(this.userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(this.tokenService, times(1)).deleteByUser(eq(user));
    verify(this.tokenService, times(1)).flush();
    verify(this.tokenService, times(1)).save(eq(user), anyString(), eq(bCryptPasswordEncoder));
    verify(this.signUpConfirmationService, times(1)).send(any(EmailCodeDto.class));
    verify(this.passwordRecoveryEmailService, never()).send(any());
  }

  @Test
  @DisplayName("Should not resend sign up confirmation when user with email is already verified")
  void shouldErrorWhenUserWithThisEmailIsAlreadyVerified() {
    User user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));

    when(this.userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));

    this.userService.resendSignUpConfirmation(new EmailValueRequestDto(TEST_EMAIL));

    verify(this.userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(this.tokenService, never()).save(any(), any(), any());
    verify(this.signUpConfirmationService, never()).send(any());
    verify(this.tokenService, never()).deleteByUser(any());
    verify(this.tokenService, never()).flush();
  }

  @Test
  @DisplayName("Should find user by ID when user exists")
  void shouldFindAnUserWhenUserWithIdExists() {
    User user = UserUtils.create();
    user.setId(TEST_USER_ID);

    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.of(user));

    User foundUser = userService.findById(TEST_USER_ID);

    assertNotNull(foundUser);
    assertEquals(TEST_USER_ID, foundUser.getId());
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user not found by ID")
  void shouldThrowsExceptionWhenUserNotBeFound() {
    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> userService.findById(TEST_USER_ID));
    assertEquals("User not found.", exception.getMessage());
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
  }

  @Test
  @DisplayName("Should mark user as deleted (soft delete)")
  void shouldMarkUserAsDeleted() {
    User user = UserUtils.create();
    user.setId(TEST_USER_ID);
    user.setDeletedAt(null);

    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);

    userService.deleteById(TEST_USER_ID);

    assertNotNull(user.getDeletedAt());
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
    verify(userRepository, times(1)).save(eq(user));
  }

  @Test
  @DisplayName("Should not attempt to delete when user not found for soft delete")
  void shouldNotDeleteUserWhenNotFound() {
    when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

    userService.deleteById(TEST_USER_ID);

    verify(userRepository, times(1)).findById(anyLong());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Should update password successfully when data is valid")
  void shouldUpdatePasswordWhenDataIsValid() {
    User user = UserUtils.create();
    user.setId(TEST_USER_ID);
    user.setPassword(ENCODED_PASSWORD);

    PasswordRequestDto passwordRequestDto = new PasswordRequestDto(TEST_PASSWORD, "newStrongPassword");

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", String.valueOf(TEST_USER_ID))));
    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.of(user));
    when(bCryptPasswordEncoder.matches(eq(TEST_PASSWORD), eq(ENCODED_PASSWORD))).thenReturn(true);
    when(bCryptPasswordEncoder.encode(eq("newStrongPassword"))).thenReturn("newEncodedPasswordHash");
    when(this.userRepository.save(any(User.class))).thenReturn(user);

    userService.updatePassword(passwordRequestDto);

    verify(jwtService, times(1)).getClaims();
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
    verify(bCryptPasswordEncoder, times(1)).matches(eq(TEST_PASSWORD), eq(ENCODED_PASSWORD));
    verify(bCryptPasswordEncoder, times(1)).encode(eq("newStrongPassword"));
    verify(userRepository, times(1)).save(eq(user));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user not found for password update")
  void shouldThrowsExceptionWhenUserNotFound() {
    PasswordRequestDto passwordRequestDto = new PasswordRequestDto(TEST_PASSWORD, "newPassword");

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", String.valueOf(TEST_USER_ID))));
    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> userService.updatePassword(passwordRequestDto));

    assertEquals("User not found.", exception.getMessage());
    verify(jwtService, times(1)).getClaims();
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
    verify(userRepository, never()).save(any(User.class));
    verify(bCryptPasswordEncoder, never()).matches(anyString(), anyString());
    verify(bCryptPasswordEncoder, never()).encode(anyString());
  }

  @Test
  @DisplayName("Should throw BadCredentialsException when current password does not match for update")
  void shouldThrowsExceptionWhenCurrentPasswordNotMatches() {
    PasswordRequestDto passwordRequestDto = new PasswordRequestDto(TEST_PASSWORD, "newPassword");
    User user = UserUtils.create();
    user.setId(TEST_USER_ID);
    user.setPassword(ENCODED_PASSWORD);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", String.valueOf(TEST_USER_ID))));
    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.of(user));
    when(bCryptPasswordEncoder.matches(eq(TEST_PASSWORD), eq(ENCODED_PASSWORD))).thenReturn(false);

    BadCredentialsException exception = assertThrows(BadCredentialsException.class,
        () -> userService.updatePassword(passwordRequestDto));

    assertEquals("Current password is not valid.", exception.getMessage());
    verify(jwtService, times(1)).getClaims();
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
    verify(bCryptPasswordEncoder, times(1)).matches(eq(TEST_PASSWORD), eq(ENCODED_PASSWORD));
    verify(userRepository, never()).save(any(User.class));
    verify(bCryptPasswordEncoder, never()).encode(anyString());
  }

  @Test
  @DisplayName("Should return UserResponseDto when user profile is updated successfully")
  void shouldUpdateUserProfileWhenUserExists() {
    User user = UserUtils.create();
    user.setId(TEST_USER_ID);

    String newName = "Updated Name";
    String newBio = "Updated Bio content";
    GenderEnum newGender = GenderEnum.FEMALE;
    ProfileRequestDto profileRequestDto = new ProfileRequestDto(newName, newBio, newGender);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", String.valueOf(TEST_USER_ID))));
    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);

    UserResponseDto result = userService.update(profileRequestDto);

    assertNotNull(result);
    assertEquals(newName, result.name());
    assertEquals(newBio, result.bio());
    assertEquals(newGender, result.gender());
    verify(jwtService, times(1)).getClaims();
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
    verify(userRepository, times(1)).save(eq(user));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user not found for profile update")
  void shouldBeThrowResourceNotFoundExceptionWhenUserNotExists() {
    String newName = "New Name";
    String newBio = "New Bio content";
    GenderEnum newGender = GenderEnum.FEMALE;
    ProfileRequestDto profileRequestDto = new ProfileRequestDto(newName, newBio, newGender);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", String.valueOf(TEST_USER_ID))));
    when(userRepository.findById(eq(TEST_USER_ID))).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
      userService.update(profileRequestDto);
    });

    assertEquals("User not found.", exception.getMessage());
    verify(jwtService, times(1)).getClaims();
    verify(userRepository, times(1)).findById(eq(TEST_USER_ID));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Should throw ValidationException when user contact is not validated during password reset")
  void shouldThrowValidationExceptionWhenUserContactIsNotValidated() {
    User user = UserUtils.create();
    Contact contact = ContactUtils.create(user);
    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    when(userRepository.findByEmail(eq(TEST_EMAIL))).thenReturn(Optional.of(user));

    ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto(TEST_EMAIL, TEST_PASSWORD, VALID_CODE);
    ValidationException exception = assertThrows(ValidationException.class,
        () -> userService.resetPassword(requestDto));

    assertEquals("The provided contact has not been validated.", exception.getMessage());
    verify(userRepository, times(1)).findByEmail(eq(TEST_EMAIL));
    verify(tokenService, never()).validateToken(any(), any(), any());
    verify(bCryptPasswordEncoder, never()).encode(anyString());
    verify(tokenService, never()).deleteByUser(any());
    verify(userRepository, never()).save(any(User.class));
  }
}
