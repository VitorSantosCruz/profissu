package br.com.conectabyte.profissu.utils;

import java.time.LocalDateTime;

import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.GenderEnum;

public class UserUtils {
  public static User createUser() {
    var user = new User();

    user.setCreatedAt(LocalDateTime.now());
    user.setName("Test");
    user.setPassword("$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G"); // admin
    user.setGender(GenderEnum.MALE);

    return user;
  }
}
