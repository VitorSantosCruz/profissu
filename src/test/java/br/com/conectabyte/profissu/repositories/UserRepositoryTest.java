package br.com.conectabyte.profissu.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {
  @Autowired
  private UserRepository userRepository;

  @Test
  void shouldReturnUserWhenEmailIsValid() {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));
    userRepository.save(user);
    final var optionalUser = userRepository.findByEmail(email);

    assertTrue(optionalUser.isPresent());
    assertTrue(optionalUser.get().getContacts().stream()
        .filter(c -> c.getValue().equals(email) && c.getType().equals(ContactTypeEnum.EMAIL) && c.isStandard()
            && c.getVerificationCompletedAt() != null)
        .findAny().isPresent());
  }

  @Test
  void shouldNotFindUserIfEmailDoesNotExist() {
    final var email = "invalid@conectabyte.com.br";
    final var optionalUser = userRepository.findByEmail(email);

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldNotFindUserWithoutEmailType() {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    contact.setType(ContactTypeEnum.PHONE);
    user.setContacts(List.of(contact));
    userRepository.save(user);
    final var optionalUser = userRepository.findByEmail(email);

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldNotFindUserWithoutDefaultEmail() {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    contact.setStandard(false);
    user.setContacts(List.of(contact));
    userRepository.save(user);
    final var optionalUser = userRepository.findByEmail(email);

    assertTrue(optionalUser.isEmpty());
  }
}
