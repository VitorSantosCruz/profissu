package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.Role;

public class RoleUtils {
  public static Role create() {
    return create("TEST");
  }

  public static Role create(String name) {
    var role = new Role();

    role.setId(0L);
    role.setName(name);

    return role;
  }
}
