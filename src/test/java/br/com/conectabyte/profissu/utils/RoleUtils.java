package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.Role;

public class RoleUtils {
  public static Role createRole() {
    return createRole("TEST");
  }

  public static Role createRole(String name) {
    var role = new Role();

    role.setName(name);

    return role;
  }
}
