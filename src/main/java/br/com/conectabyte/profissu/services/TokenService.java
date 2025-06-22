package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
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
  private final ProfissuProperties profissuProperties;

  public Token save(Token token) {
    log.debug("Saving token for user ID: {}", token.getUser().getId());

    final var savedToken = this.tokenRepository.save(token);

    log.info("Token saved with ID: {} for user ID: {}", savedToken.getId(), savedToken.getUser().getId());
    return savedToken;
  }

  public void delete(Token token) {
    log.debug("Deleting token with ID: {}", token.getId());
    this.tokenRepository.delete(token);
    log.info("Token with ID: {} deleted successfully.", token.getId());
  }

  public void flush() {
    log.debug("Flushing token repository.");
    tokenRepository.flush();
  }

  @Transactional
  public void deleteByUser(User user) {
    log.debug("Attempting to delete token for user ID: {}", user.getId());

    final var token = user.getToken();

    if (token == null) {
      log.debug("No token found to delete for user ID: {}", user.getId());
      return;
    }

    token.getUser().setToken(null);
    this.delete(token);
    log.info("Token for user ID: {} deleted via deleteByUser method.", user.getId());
  }

  @Transactional
  public void save(User user, String code, PasswordEncoder passwordEncoder) {
    log.debug("Saving new token for user ID: {}", user.getId());

    final var token = Token.builder()
        .value(passwordEncoder.encode(code))
        .user(user)
        .build();

    this.deleteByUser(user);
    this.save(token);
    log.info("New token saved for user ID: {}", user.getId());
  }

  public String validateToken(User user, String email, String code) {
    log.debug("Validating token for user email: {}", email);

    final var token = user.getToken();

    if (token == null) {
      log.warn("Reset code not found for user with this e-mail: {}", email);
      return "Missing reset code for user with this e-mail.";
    }

    final var isValidToken = token.isValid(code, bCryptPasswordEncoder);

    if (!isValidToken) {
      log.warn("Reset code is invalid for user email: {}", email);
      return "Reset code is invalid.";
    }

    final var expiresIn = profissuProperties.getProfissu().getToken().getExpiresIn();
    final var isExpiredToken = token.getCreatedAt().plusMinutes(expiresIn).isBefore(LocalDateTime.now());

    if (isExpiredToken) {
      log.warn("Reset code is expired for user email: {}. Expires in: {} minutes.", email, expiresIn);
      this.deleteByUser(user);
      return "Reset code is expired.";
    }

    log.debug("Token for user email: {} is valid.", email);
    return null;
  }
}
