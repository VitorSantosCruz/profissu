package br.com.conectabyte.profissu.services;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LoginService {
  private final TokenService tokenService;
  private final UserService userService;
  private final BCryptPasswordEncoder passwordEncoder;

  public LoginResponseDto login(LoginRequestDto loginRequest) {
    var optionalUser = userService.findByEmail(loginRequest.email());
    if (optionalUser.isEmpty() || !optionalUser.get().isValidPassword(loginRequest, passwordEncoder)) {
      throw new BadCredentialsException("credentials.is.not.valid");
    }

    return tokenService.create(optionalUser.get());
  }
}
