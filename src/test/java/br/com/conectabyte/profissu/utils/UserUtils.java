package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.GenderEnum;

public class UserUtils {
  public static User create() {
    final var user = new User();

    user.setName("Test Test");
    user.setBio("Bio");
    user.setPassword("@Admin123");
    user.setGender(GenderEnum.MALE);

    return user;
  }
}
