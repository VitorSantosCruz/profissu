package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.GenderEnum;

public class UserUtils {
  public static User create() {
    var user = new User();

    user.setId(0L);
    user.setName("Test Test");
    user.setBio("Bio");
    user.setPassword("@Admin123");
    user.setGender(GenderEnum.MALE);

    return user;
  }
}
