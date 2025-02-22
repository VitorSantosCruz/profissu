package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.PasswordRecoveryRequestDto;
import br.com.conectabyte.profissu.dtos.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.dtos.ResetPasswordResponseDto;
import br.com.conectabyte.profissu.dtos.UserRequestDto;
import br.com.conectabyte.profissu.dtos.UserResponseDto;
import br.com.conectabyte.profissu.entities.Profile;
import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.RoleEnum;
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
  public UserResponseDto save(UserRequestDto userDto) {
    final var userToBeSaved = userMapper.userRequestDtoToUser(userDto);
    userToBeSaved.setPassword(bCryptPasswordEncoder.encode(userDto.password()));
    userToBeSaved.getContacts().forEach(c -> c.setUser(userToBeSaved));
    userToBeSaved.getAddresses().forEach(a -> a.setUser(userToBeSaved));
    userToBeSaved.setProfile(Profile.builder().user(userToBeSaved).build());
    userToBeSaved.setRoles(
        Set.of(roleService.findByName(RoleEnum.USER.toString())
            .orElse(Role.builder().name("USER").build())));

    final var user = this.save(userToBeSaved);

    return userMapper.userToUserResponseDto(user);
  }

  @Async
  public void recoverPassword(PasswordRecoveryRequestDto passwordRecoveryRequestDto) {
    final var email = passwordRecoveryRequestDto.email();
    final var optionalUser = this.findByEmail(email);
    final var resetCode = UUID.randomUUID().toString().split("-")[1];

    if (optionalUser.isEmpty()) {
      log.warn("No user found with this e-mail: {}", email);
      return;
    }

    final var token = Token.builder()
        .value(bCryptPasswordEncoder.encode(resetCode))
        .user(optionalUser.get())
        .build();
    this.tokenService.deleteByUser(optionalUser.get());
    this.tokenService.save(token);

    try {
      emailService.sendPasswordRecoveryEmail(email, resetCode);
      log.info("E-mail successfully sent to {}", email);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", email, e.getMessage());
    }
  }

  public ResetPasswordResponseDto resetPassword(ResetPasswordRequestDto resetPasswordRequestDto) {
    final var optionalUser = this.findByEmail(resetPasswordRequestDto.email());

    if (optionalUser.isEmpty()) {
      log.warn("No user found with this e-mail: {}", resetPasswordRequestDto.email());
      return new ResetPasswordResponseDto(HttpStatus.BAD_REQUEST.value(), "Reset code is invalid.");
    }

    final var user = optionalUser.get();
    final var token = user.getToken();

    if (token == null) {
      log.warn("Reset code not found for user with this e-mail: {}", resetPasswordRequestDto.email());
      return new ResetPasswordResponseDto(HttpStatus.BAD_REQUEST.value(), "Reset code is invalid.");
    }

    final var isValidToken = bCryptPasswordEncoder.matches(resetPasswordRequestDto.resetCode(),
        token.getValue());

    if (!isValidToken) {
      log.warn("Reset code is invalid.");
      return new ResetPasswordResponseDto(HttpStatus.BAD_REQUEST.value(), "Reset code is invalid.");
    }

    final var isExpiredToken = token.getCreatedAt().plusMinutes(1).isBefore(LocalDateTime.now());

    if (isExpiredToken) {
      log.warn("Reset code is expired.");
      this.tokenService.deleteByUser(optionalUser.get());
      return new ResetPasswordResponseDto(HttpStatus.BAD_REQUEST.value(), "Reset code is expired.");
    }

    user.setPassword(bCryptPasswordEncoder.encode(resetPasswordRequestDto.password()));

    this.tokenService.deleteByUser(optionalUser.get());

    return new ResetPasswordResponseDto(HttpStatus.OK.value(), "Password was updated.");
  }
}
