package br.com.conectabyte.profissu.properties;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Token {
  @JsonProperty("expires-in")
  private Long expiresIn;
}
