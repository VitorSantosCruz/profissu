package br.com.conectabyte.profissu.services;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.conectabyte.profissu.dtos.UserRequestDto;
import br.com.conectabyte.profissu.dtos.UserResponseDto;
import br.com.conectabyte.profissu.entities.Profile;
import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.UserRepository;
import jakarta.mail.MessagingException;
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

  @Value("${profissu.url}")
  private String profissuUrl;

  private final UserMapper userMapper = UserMapper.INSTANCE;

  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
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

    final var user = userRepository.save(userToBeSaved);

    return userMapper.userToUserResponseDto(user);
  }

  @Async
  public void recoverPassword(String email) {
    var optionalUser = this.findByEmail(email);

    if (optionalUser.isEmpty()) {
      log.warn("No user found with this e-mail: {}", email);
      return;
    }

    try {
      emailService.sendPasswordRecoveryEmail(email);
      log.info("E-mail successfully sent to {}", email);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", email, e.getMessage());
    }
  }
}
