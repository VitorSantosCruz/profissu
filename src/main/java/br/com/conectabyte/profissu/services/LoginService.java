package br.com.conectabyte.profissu.services;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {
  private final JwtService jwtService;
  private final UserService userService;
  private final BCryptPasswordEncoder passwordEncoder;

  public LoginResponseDto login(LoginRequestDto loginRequest) {
    final var user = this.validate(loginRequest);

    return jwtService.createJwtToken(user);
  }

  private User validate(LoginRequestDto loginRequest) {
    User user = null;

    try {
      user = userService.findByEmail(loginRequest.email());
    } catch (Exception e) {
      log.debug(e.getMessage());
    }

    if (user == null || !user.isValidPassword(loginRequest.password(), passwordEncoder)) {
      throw new BadCredentialsException("Credentials is not valid");
    }

    user.getContacts().stream()
        .filter(c -> c.getValue().equals(loginRequest.email()))
        .filter(c -> c.getVerificationCompletedAt() == null)
        .findFirst()
        .ifPresent(c -> {
          throw new EmailNotVerifiedException("E-mail is not verified");
        });

    return user;
  }
}
