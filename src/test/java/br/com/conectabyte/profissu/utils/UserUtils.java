package br.com.conectabyte.profissu.utils;

import java.time.LocalDateTime;

import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.GenderEnum;

public class UserUtils {
  public static User createUser() {
    var user = new User();

    user.setCreatedAt(LocalDateTime.now());
    user.setName("Test");
    user.setPassword("$2y$10$pZKpygPyYuXXySPufr4VAeNrcKhxueFwXXNm.p7mvrKnUSamaXoPy"); // admin
    user.setGender(GenderEnum.MALE);

    return user;
  }
}
