package br.com.conectabyte.profissu.properties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Setter;

@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Jwt {
  @JsonProperty("expires-in")
  private Long expiresIn;

  @JsonIgnore
  private RSAPublicKey publicKeyLocation;

  @JsonIgnore
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
