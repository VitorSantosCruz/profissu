package br.com.conectabyte.profissu.properties;

import lombok.Data;

@Data
public class Security {
  private User user = new User();
}
