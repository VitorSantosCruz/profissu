package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.EmailCodeDto;
import br.com.conectabyte.profissu.dtos.request.EmailValueRequestDto;
import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.ProfileRequestDto;
import br.com.conectabyte.profissu.dtos.request.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.UserRequestDto;
import br.com.conectabyte.profissu.dtos.response.MessageValueResponseDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.UserRepository;
import br.com.conectabyte.profissu.services.email.PasswordRecoveryEmailService;
import br.com.conectabyte.profissu.services.email.SignUpConfirmationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final RoleService roleService;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final PasswordRecoveryEmailService passwordRecoveryEmailService;
  private final SignUpConfirmationService signUpConfirmationService;
  private final TokenService tokenService;
  private final JwtService jwtService;

  private final UserMapper userMapper = UserMapper.INSTANCE;

  public User findById(Long id) {
    log.debug("Attempting to find user by ID: {}", id);

    final var optionalUser = this.userRepository.findById(id);
    final var user = optionalUser.orElseThrow(() -> {
      log.warn("User with ID: {} not found.", id);
      return new ResourceNotFoundException("User not found.");
    });

    log.debug("Found user with ID: {}", user.getId());
    return user;
  }

  @Transactional
  public UserResponseDto findByIdAndReturnDto(Long id) {
    log.debug("Finding user by ID and mapping to DTO: {}", id);
    return userMapper.userToUserResponseDto(this.findById(id));
  }

  public User findByEmail(String email) {
    log.debug("Attempting to find user by email: {}", email);

    final var optionalUser = this.userRepository.findByEmail(email);
    final var user = optionalUser.orElseThrow(() -> {
      log.warn("User with email: {} not found.", email);
      return new ResourceNotFoundException("User not found.");
    });

    log.debug("Found user with email: {}", email);
    return user;
  }

  public User save(User user) {
    log.debug("Saving user with ID: {}", user.getId());

    final var savedUser = userRepository.save(user);

    log.info("User saved successfully with ID: {}", savedUser.getId());
    return savedUser;
  }

  @Transactional
  public UserResponseDto register(UserRequestDto userDto) {
    log.debug("Registering new user with email: {}", userDto.contacts().get(0).value());

    final var userToBeSaved = userMapper.userRequestDtoToUser(userDto);

    userToBeSaved.setPassword(bCryptPasswordEncoder.encode(userDto.password()));
    userToBeSaved.getContacts().forEach(c -> {
      c.setUser(userToBeSaved);
      c.setVerificationRequestedAt(LocalDateTime.now());
    });
    userToBeSaved.getAddresses().forEach(a -> a.setUser(userToBeSaved));
    userToBeSaved.setRoles(
        Set.of(roleService.findByName(RoleEnum.USER.name())
            .orElse(Role.builder().name("USER").build())));

    final var savedUser = this.save(userToBeSaved);
    final var code = UUID.randomUUID().toString().split("-")[1];

    log.debug("Generated sign up confirmation code: {}", code);
    this.tokenService.save(savedUser, code, bCryptPasswordEncoder);
    this.signUpConfirmationService.send(new EmailCodeDto(userDto.contacts().get(0).value(), code));
    log.info("New user registered with ID: {}. Sign up confirmation email sent to: {}", savedUser.getId(),
        userDto.contacts().get(0).value());

    return userMapper.userToUserResponseDto(savedUser);
  }

  @Async
  @Transactional
  public void resendSignUpConfirmation(EmailValueRequestDto emailValueRequestDto) {
    log.debug("Resending sign up confirmation for email: {}", emailValueRequestDto.email());
    sendCodeEmail(emailValueRequestDto.email(), true);
  }

  @Async
  @Transactional
  public void recoverPassword(EmailValueRequestDto emailValueRequestDto) {
    log.debug("Initiating password recovery for email: {}", emailValueRequestDto.email());
    sendCodeEmail(emailValueRequestDto.email(), false);
  }

  private void sendCodeEmail(String email, boolean isSignUp) {
    log.debug("Sending code email for email: {} (isSignUp: {})", email, isSignUp);

    User user = null;

    try {
      user = this.findByEmail(email);
    } catch (Exception e) {
      log.warn("No user found with this e-mail: {}. Skipping code email send.", email);
      return;
    }

    final var userIsVerified = user.getContacts().stream()
        .filter(c -> c.getValue().equals(email))
        .filter(c -> c.getVerificationCompletedAt() != null)
        .findFirst()
        .isPresent();

    log.debug("User with email {} is verified: {}", email, userIsVerified);

    if (isSignUp && userIsVerified) {
      log.warn("User with this e-mail: {} is already verified. Skipping sign up confirmation.", email);
      return;
    }

    if (!isSignUp && !userIsVerified) {
      log.warn("User with this e-mail: {} is not verified. Skipping password recovery.", email);
      return;
    }

    final var code = UUID.randomUUID().toString().split("-")[1];

    log.debug("Generated code for email {}: {}", email, code);
    this.tokenService.deleteByUser(user);
    this.tokenService.flush();
    this.tokenService.save(user, code, bCryptPasswordEncoder);
    log.debug("Token saved for user ID: {}", user.getId());

    if (isSignUp) {
      signUpConfirmationService.send(new EmailCodeDto(email, code));
      log.info("Sign up confirmation email sent to: {}", email);
    } else {
      passwordRecoveryEmailService.send(new EmailCodeDto(email, code));
      log.info("Password recovery email sent to: {}", email);
    }
  }

  @Transactional
  public MessageValueResponseDto resetPassword(ResetPasswordRequestDto resetPasswordRequestDto) {
    log.debug("Resetting password for email: {}", resetPasswordRequestDto.email());

    final var email = resetPasswordRequestDto.email();
    User user = null;

    try {
      user = this.findByEmail(email);
    } catch (Exception e) {
      log.warn("No user found with this e-mail: {}. Cannot reset password.", email);
      throw new ValidationException("No user found with this e-mail.");
    }

    log.debug("User found for password reset: {}", user.getId());

    final var isValidated = user.getContacts().stream()
        .anyMatch(c -> c.getVerificationCompletedAt() != null);

    if (!isValidated) {
      log.warn("The provided contact for user {} has not been validated. Cannot reset password.", user.getId());
      throw new ValidationException("The provided contact has not been validated.");
    }

    log.debug("Contact for user {} is validated.", user.getId());

    final var messageError = tokenService.validateToken(user, email, resetPasswordRequestDto.code());

    if (messageError != null) {
      log.warn("Token validation failed for user {}: {}", user.getId(), messageError);
      throw new ValidationException(messageError);
    }

    log.debug("Token validated successfully for user {}.", user.getId());
    user.setPassword(bCryptPasswordEncoder.encode(resetPasswordRequestDto.password()));
    this.tokenService.deleteByUser(user);

    log.info("Password for user ID: {} reset successfully.", user.getId());
    return new MessageValueResponseDto("Password was updated.");
  }

  @Async
  @Transactional
  public void deleteById(Long id) {
    log.debug("Attempting to soft-delete user by ID: {}", id);

    final var optionalUser = this.userRepository.findById(id);

    optionalUser.ifPresent(user -> {
      user.setDeletedAt(LocalDateTime.now());
      this.save(user);
      log.info("User with ID: {} soft-deleted successfully.", id);
    });

    if (optionalUser.isEmpty()) {
      log.warn("Attempted to soft-delete user with ID: {} but it was not found.", id);
    }
  }

  public void updatePassword(PasswordRequestDto passwordRequestDto) {
    log.debug("Updating password for current authenticated user.");

    final var id = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("Retrieved user ID from JWT: {}", id);

    final var user = this.findById(id);
    final var isValidPassword = user.isValidPassword(passwordRequestDto.currentPassword(), bCryptPasswordEncoder);

    if (!isValidPassword) {
      log.warn("Password update failed for user ID {}: Current password is not valid.", id);
      throw new BadCredentialsException("Current password is not valid.");
    }

    log.debug("Current password validated for user ID: {}", id);

    user.setUpdatedAt(LocalDateTime.now());
    user.setPassword(bCryptPasswordEncoder.encode(passwordRequestDto.newPassword()));
    this.save(user);
    log.info("Password for user ID: {} updated successfully.", id);
  }

  public UserResponseDto update(ProfileRequestDto profileRequestDto) {
    log.debug("Updating profile for current authenticated user with data: {}", profileRequestDto);

    final var id = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("Retrieved user ID from JWT: {}", id);

    final var user = this.findById(id);

    user.setUpdatedAt(LocalDateTime.now());
    user.setName(profileRequestDto.name());
    user.setBio(profileRequestDto.bio());
    user.setGender(profileRequestDto.gender());

    final var savedUser = this.save(user);

    log.info("Profile for user ID: {} updated successfully.", savedUser.getId());
    return userMapper.userToUserResponseDto(savedUser);
  }
}
