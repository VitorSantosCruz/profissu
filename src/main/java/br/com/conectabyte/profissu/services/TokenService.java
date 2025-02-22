package br.com.conectabyte.profissu.services;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.repositories.TokenRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {
  private final TokenRepository tokenRepository;

  public Token save(Token token) {
    return this.tokenRepository.save(token);
  }

  public void delete(Token token) {
    this.tokenRepository.delete(token);
  }

  public void deleteByUser(User user) {
    final var token = user.getToken();

    if (token == null) {
      return;
    }

    token.getUser().setToken(null);

    this.delete(token);
  }
}
