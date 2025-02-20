package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.GenderEnum;

public class UserUtils {
  public static User create() {
    var user = new User();

    user.setName("Test Test");
    user.setPassword("$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G"); // admin
    user.setGender(GenderEnum.MALE);

    return user;
  }
}
