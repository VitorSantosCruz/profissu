package br.com.conectabyte.profissu.properties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import lombok.Setter;

@Setter
public class Jwt {
  private Long expiresIn;
  private RSAPublicKey publicKeyLocation;
  private RSAPrivateKey privateKeyLocation;

  public Long getExpiresIn() {
    return expiresIn;
  }

  public RSAPublicKey getPublicKey() {
    return publicKeyLocation;
  }

  public RSAPrivateKey getPrivateKey() {
    return privateKeyLocation;
  }
}
