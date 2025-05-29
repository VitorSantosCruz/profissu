package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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

  private final UserMapper userMapper = UserMapper.INSTANCE;

  public User findById(Long id) {
    final var optionalUser = this.userRepository.findById(id);
    final var user = optionalUser.orElseThrow(() -> new ResourceNotFoundException("User not found."));

    return user;
  }

  @Transactional
  public UserResponseDto findByIdAndReturnDto(Long id) {
    return userMapper.userToUserResponseDto(this.findById(id));
  }

  public User findByEmail(String email) {
    final var optionalUser = this.userRepository.findByEmail(email);
    final var user = optionalUser.orElseThrow(() -> new ResourceNotFoundException("User not found."));

    return user;
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
    userToBeSaved.setRoles(
        Set.of(roleService.findByName(RoleEnum.USER.name())
            .orElse(Role.builder().name("USER").build())));

    final var savedUser = this.save(userToBeSaved);
    final var code = UUID.randomUUID().toString().split("-")[1];

    this.tokenService.save(savedUser, code, bCryptPasswordEncoder);
    this.signUpConfirmationService.send(new EmailCodeDto(userDto.contacts().get(0).value(), code));

    return userMapper.userToUserResponseDto(savedUser);
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
    User user = null;

    try {
      user = this.findByEmail(email);
    } catch (Exception e) {
      log.warn("No user found with this e-mail: {}", email);
      return;
    }

    final var userIsVerified = user.getContacts().stream()
        .filter(c -> c.getValue().equals(email))
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

    if (isSignUp) {
      signUpConfirmationService.send(new EmailCodeDto(email, code));
    } else {
      passwordRecoveryEmailService.send(new EmailCodeDto(email, code));
    }
  }

  public MessageValueResponseDto resetPassword(ResetPasswordRequestDto resetPasswordRequestDto) {
    final var email = resetPasswordRequestDto.email();
    User user = null;

    try {
      user = this.findByEmail(email);
    } catch (Exception e) {
      log.warn("No user found with this e-mail: {}", email);
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), "No user found with this e-mail.");
    }

    final var messageError = tokenService.validateToken(user, email, resetPasswordRequestDto.code());

    if (messageError != null) {
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), messageError);
    }

    user.setPassword(bCryptPasswordEncoder.encode(resetPasswordRequestDto.password()));
    this.tokenService.deleteByUser(user);

    return new MessageValueResponseDto(HttpStatus.OK.value(), "Password was updated.");
  }

  @Async
  public void deleteById(Long id) {
    final var optionalUser = this.userRepository.findById(id);

    optionalUser.ifPresent(user -> {
      user.setDeletedAt(LocalDateTime.now());
      this.save(user);
    });
  }

  public void updatePassword(Long id, PasswordRequestDto passwordRequestDto) {
    final var optionalUser = this.userRepository.findById(id);
    final var user = optionalUser.orElseThrow(() -> new ResourceNotFoundException("User not found."));
    final var isValidPassword = user.isValidPassword(passwordRequestDto.currentPassword(), bCryptPasswordEncoder);

    if (!isValidPassword) {
      throw new BadCredentialsException("Current password is not valid.");
    }

    user.setUpdatedAt(LocalDateTime.now());
    user.setPassword(bCryptPasswordEncoder.encode(passwordRequestDto.newPassword()));
    this.save(user);
  }

  public UserResponseDto update(Long id, ProfileRequestDto profileRequestDto) {
    final var optionalUser = this.userRepository.findById(id);
    final var user = optionalUser.orElseThrow(() -> new ResourceNotFoundException("User not found."));

    user.setUpdatedAt(LocalDateTime.now());
    user.setName(profileRequestDto.name());
    user.setBio(profileRequestDto.bio());
    user.setGender(profileRequestDto.gender());

    final var savedUser = this.save(user);

    return userMapper.userToUserResponseDto(savedUser);
  }
}
