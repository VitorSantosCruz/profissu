package br.com.conectabyte.profissu.services;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
  private final JwtService jwtService;
  private final UserService userService;
  private final BCryptPasswordEncoder passwordEncoder;

  @Transactional
  public LoginResponseDto login(LoginRequestDto loginRequest) {
    var optionalUser = userService.findByEmail(loginRequest.email());

    this.validate(optionalUser, loginRequest);

    return jwtService.createJwtToken(optionalUser.get());
  }

  private void validate(Optional<User> optionalUser, LoginRequestDto loginRequest) {
    if (optionalUser.isEmpty() || !optionalUser.get().isValidPassword(loginRequest, passwordEncoder)) {
      throw new BadCredentialsException("Credentials is not valid");
    }

    optionalUser.get().getContacts().stream()
        .filter(c -> c.getValue().equals(loginRequest.email()))
        .filter(c -> c.getVerificationCompletedAt() == null)
        .findFirst()
        .ifPresent(c -> {
          throw new EmailNotVerifiedException("E-mail is not verified");
        });
  }
}
