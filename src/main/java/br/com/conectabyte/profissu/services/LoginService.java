package br.com.conectabyte.profissu.services;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {
  private final JwtService jwtService;
  private final UserService userService;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public LoginResponseDto login(LoginRequestDto loginRequest) {
    log.info("Attempting login for email: {}", loginRequest.email());

    final var user = this.validate(loginRequest);

    log.info("Login successful for user ID: {}", user.getId());
    return jwtService.createJwtToken(user);
  }

  private User validate(LoginRequestDto loginRequest) {
    log.debug("Validating credentials for email: {}", loginRequest.email());
    User user = null;

    try {
      user = userService.findByEmail(loginRequest.email());
      log.debug("User found by email: {}", loginRequest.email());
    } catch (Exception e) {
      log.debug("User not found by email {}: {}", loginRequest.email(), e.getMessage());
    }

    if (user == null || !user.isValidPassword(loginRequest.password(), passwordEncoder)) {
      log.warn("Login failed for email {}: Invalid credentials.", loginRequest.email());
      throw new BadCredentialsException("Credentials is not valid");
    }

    user.getContacts().stream()
        .filter(c -> c.getValue().equals(loginRequest.email()))
        .filter(c -> c.getVerificationCompletedAt() == null)
        .findFirst()
        .ifPresent(c -> {
          log.warn("Login failed for email {}: Email not verified.", loginRequest.email());
          throw new EmailNotVerifiedException("E-mail is not verified");
        });

    log.debug("Credentials validated successfully for user ID: {}", user.getId());
    return user;
  }
}
