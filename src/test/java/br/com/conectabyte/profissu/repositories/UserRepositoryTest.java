package br.com.conectabyte.profissu.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import br.com.conectabyte.profissu.enums.GenderEnum;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {
  @Autowired
  private UserRepository userRepository;

  @Test
  void shouldBeReturnUserWhenEmailIsValid() {
    final String email = "test@conectabyte.com.br";
    this.createUserWith(email);
    Optional<User> optionalUser = userRepository.findByEmail(email);

    System.out.println(optionalUser);
    assertTrue(optionalUser.isPresent());
    assertTrue(optionalUser.get().getContacts().stream()
        .filter(c -> c.getValue().equals(email) && c.getType().equals(ContactTypeEnum.EMAIL) && c.isStandard())
        .findAny().isPresent());
  }

  @Test
  void shouldNotBeReturnUserWhenEmailIsInvalid() {
    final String email = "invalid@conectabyte.com.br";
    Optional<User> optionalUser = userRepository.findByEmail(email);

    assertTrue(optionalUser.isEmpty());
  }

  private void createUserWith(String email) {
    var contact = new Contact();
    var user = new User();

    user.setCreatedAt(LocalDateTime.now());
    user.setGender(GenderEnum.MALE);
    user.setName("Test");
    user.setPassword("$2y$10$pZKpygPyYuXXySPufr4VAeNrcKhxueFwXXNm.p7mvrKnUSamaXoPy"); // admin

    contact.setCreatedAt(LocalDateTime.now());
    contact.setStandard(true);
    contact.setType(ContactTypeEnum.EMAIL);
    contact.setValue(email);
    contact.setUser(user);

    user.setContacts(List.of(contact));

    var suser = userRepository.save(user);
    System.out.println(suser);
  }
}
