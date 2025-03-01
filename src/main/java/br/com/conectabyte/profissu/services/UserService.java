package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.EmailValueRequestDto;
import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.SignUpConfirmationRequestDto;
import br.com.conectabyte.profissu.dtos.request.UserRequestDto;
import br.com.conectabyte.profissu.dtos.response.MessageValueResponseDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.entities.Profile;
import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.UserRepository;
import jakarta.mail.MessagingException;
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
  private final EmailService emailService;
  private final TokenService tokenService;

  @Value("${profissu.url}")
  private String profissuUrl;

  private final UserMapper userMapper = UserMapper.INSTANCE;

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public User save(User user) {
    return userRepository.save(user);
  }

  @Transactional
  public UserResponseDto register(UserRequestDto userDto) {
    final var userToBeSaved = userMapper.userRequestDtoToUser(userDto);
    userToBeSaved.setPassword(bCryptPasswordEncoder.encode(userDto.password()));
    userToBeSaved.getContacts().forEach(c -> {
      c.setUser(userToBeSaved);
      c.setVerificationRequestedAt(LocalDateTime.now());
    });
    userToBeSaved.getAddresses().forEach(a -> a.setUser(userToBeSaved));
    userToBeSaved.setProfile(Profile.builder().user(userToBeSaved).build());
    userToBeSaved.setRoles(
        Set.of(roleService.findByName(RoleEnum.USER.name())
            .orElse(Role.builder().name("USER").build())));

    final var user = this.save(userToBeSaved);
    final var code = UUID.randomUUID().toString().split("-")[1];

    this.tokenService.save(user, code, bCryptPasswordEncoder);
    this.emailService.sendSignUpConfirmation(userDto.contacts().get(0).value(), code);

    return userMapper.userToUserResponseDto(user);
  }

  @Transactional
  public MessageValueResponseDto signUpConfirmation(SignUpConfirmationRequestDto signUpConfirmationRequestDto) {
    final var email = signUpConfirmationRequestDto.email();
    final var optionalUser = this.findByEmail(email);

    if (optionalUser.isEmpty()) {
      log.warn("No user found with this e-mail: {}", email);
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), "No user found with this e-mail.");
    }

    final var user = optionalUser.get();
    final var messageError = validateToken(user, email, signUpConfirmationRequestDto.code());

    if (messageError != null) {
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), messageError);
    }

    user.getContacts().stream().filter(c -> c.isStandard()).findFirst()
        .ifPresent(c -> c.setVerificationCompletedAt(LocalDateTime.now()));
    this.tokenService.deleteByUser(user);
    this.save(user);

    return new MessageValueResponseDto(HttpStatus.OK.value(), "Sign up was confirmed.");
  }

  @Async
  @Transactional
  public void resendSignUpConfirmation(EmailValueRequestDto emailValueRequestDto) {
    sendCodeEmail(emailValueRequestDto.email(), true);
  }

  @Async
  @Transactional
  public void recoverPassword(EmailValueRequestDto emailValueRequestDto) {
    sendCodeEmail(emailValueRequestDto.email(), false);
  }

  private void sendCodeEmail(String email, boolean isSignUp) {
    final var optionalUser = this.findByEmail(email);

    if (optionalUser.isEmpty()) {
      log.warn("No user found with this e-mail: {}", email);
      return;
    }

    final var user = optionalUser.get();
    final var userIsVerified = user.getContacts().stream()
        .filter(c -> c.isStandard())
        .filter(c -> c.getVerificationCompletedAt() != null)
        .findFirst()
        .isPresent();

    if (isSignUp && userIsVerified) {
      log.warn("User with this e-mail: {} is already verified.", email);
      return;
    }

    if (!isSignUp && !userIsVerified) {
      log.warn("User with this e-mail: {} is not verified.", email);
      return;
    }

    final var code = UUID.randomUUID().toString().split("-")[1];
    this.tokenService.save(user, code, bCryptPasswordEncoder);

    try {
      if (isSignUp) {
        user.getContacts().stream().filter(c -> c.isStandard()).findFirst()
            .ifPresent(c -> c.setVerificationCompletedAt(LocalDateTime.now()));
        emailService.sendSignUpConfirmation(email, code);
      } else {
        emailService.sendPasswordRecoveryEmail(email, code);
      }
      log.info("E-mail successfully sent to {}", email);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", email, e.getMessage());
    }
  }

  public MessageValueResponseDto resetPassword(ResetPasswordRequestDto resetPasswordRequestDto) {
    final var email = resetPasswordRequestDto.email();
    final var optionalUser = this.findByEmail(email);

    if (optionalUser.isEmpty()) {
      log.warn("No user found with this e-mail: {}", email);
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), "No user found with this e-mail.");
    }

    final var user = optionalUser.get();
    final var messageError = validateToken(user, email, resetPasswordRequestDto.code());

    if (messageError != null) {
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), messageError);
    }

    user.setPassword(bCryptPasswordEncoder.encode(resetPasswordRequestDto.password()));
    this.tokenService.deleteByUser(user);

    return new MessageValueResponseDto(HttpStatus.OK.value(), "Password was updated.");
  }

  private String validateToken(User user, String email, String code) {
    final var token = user.getToken();

    if (token == null) {
      log.warn("Reset code not found for user with this e-mail: {}", email);
      return "Missing reset code for user with this e-mail.";
    }

    final var isValidToken = token.isValid(code, bCryptPasswordEncoder);

    if (!isValidToken) {
      log.warn("Reset code is invalid.");
      return "Reset code is invalid.";
    }

    final var isExpiredToken = token.getCreatedAt().plusMinutes(1).isBefore(LocalDateTime.now());

    if (isExpiredToken) {
      log.warn("Reset code is expired.");
      this.tokenService.deleteByUser(user);
      return "Reset code is expired.";
    }

    return null;
  }

  @Transactional
  public UserResponseDto findById(Long id) {
    final var optionalUser = this.userRepository.findById(id);
    final var user = optionalUser.orElseThrow(() -> new ResourceNotFoundException("User not found."));

    return userMapper.userToUserResponseDto(user);
  }

  @Async
  public void deleteById(Long id) {
    final var optionalUser = this.userRepository.findById(id);

    optionalUser.ifPresent(user -> {
      user.setDeletedAt(LocalDateTime.now());
      userRepository.save(user);
    });
  }

  public void updatePassword(Long id, PasswordRequestDto passwordRequestDto) {
    final var optionalUser = this.userRepository.findById(id);
    final var user = optionalUser.orElseThrow(() -> new ResourceNotFoundException("User not found."));
    final var isValidPassword = user.isValidPassword(passwordRequestDto.currentPassword(), bCryptPasswordEncoder);

    if (!isValidPassword) {
      throw new BadCredentialsException("Current password is not valid.");
    }

    user.setPassword(bCryptPasswordEncoder.encode(passwordRequestDto.newPassword()));
    this.save(user);
  }
}
