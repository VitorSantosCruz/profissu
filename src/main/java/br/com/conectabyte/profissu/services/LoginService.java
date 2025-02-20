package br.com.conectabyte.profissu.services;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
  private final TokenService tokenService;
  private final UserService userService;
  private final BCryptPasswordEncoder passwordEncoder;

  public LoginResponseDto login(LoginRequestDto loginRequest) {
    var optionalUser = userService.findByEmail(loginRequest.email());

    this.validate(optionalUser, loginRequest, passwordEncoder);

    return tokenService.create(optionalUser.get());
  }

  private void validate(Optional<User> optionalUser, LoginRequestDto loginRequest,
      BCryptPasswordEncoder passwordEncoder2) {
    if (optionalUser.isEmpty() || !optionalUser.get().isValidPassword(loginRequest, passwordEncoder)) {
      throw new BadCredentialsException("Credentials is not valid");
    }

    optionalUser.get().getContacts().stream()
        .filter(c -> c.getValue().equals(loginRequest.email()) && c.getVerificationCompletedAt() == null)
        .findFirst()
        .ifPresent(c -> {
          throw new EmailNotVerifiedException("E-mail is not verified");
        });
  }
}
