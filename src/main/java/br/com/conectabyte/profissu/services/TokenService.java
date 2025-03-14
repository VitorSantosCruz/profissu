package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.repositories.TokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
  private final TokenRepository tokenRepository;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  public Token save(Token token) {
    return this.tokenRepository.save(token);
  }

  public void delete(Token token) {
    this.tokenRepository.delete(token);
  }

  @Transactional
  public void deleteByUser(User user) {
    final var token = user.getToken();

    if (token == null) {
      return;
    }

    token.getUser().setToken(null);

    this.delete(token);
  }

  @Transactional
  public void save(User user, String code, PasswordEncoder passwordEncoder) {
    final var token = Token.builder()
        .value(passwordEncoder.encode(code))
        .user(user)
        .build();
    this.deleteByUser(user);
    this.save(token);
  }

  public String validateToken(User user, String email, String code) {
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
      this.deleteByUser(user);
      return "Reset code is expired.";
    }

    return null;
  }
}
