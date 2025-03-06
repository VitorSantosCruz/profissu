package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.Token;
import br.com.conectabyte.profissu.entities.User;

public class TokenUtils {
  public static Token create(User user) {
    final var token = new Token();

    token.setId(0L);
    token.setValue("CODE");
    token.setUser(user);

    return token;
  }
}
