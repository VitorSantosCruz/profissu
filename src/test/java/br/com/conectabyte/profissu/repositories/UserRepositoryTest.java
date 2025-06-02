package br.com.conectabyte.profissu.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

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
        .filter(c -> c.isStandard())
        .filter(c -> c.getVerificationCompletedAt() != null)
        .filter(c -> c.getValue().equals(email))
        .findAny().isPresent());
  }

  @Test
  void shouldNotFindUserIfEmailDoesNotExist() {
    final var email = "invalid@conectabyte.com.br";
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

  @Test
  void shouldReturnUserWhenHaveAnUserWithInformedId() {
    final var user = UserUtils.create();

    user.setContacts(List.of(ContactUtils.create(user)));

    final var savedUser = userRepository.save(user);
    final var optionalUser = userRepository.findById(savedUser.getId());

    assertTrue(optionalUser.isPresent());
  }

  @Test
  void shouldNotFindUserWhenThenIsDeleted() {
    final var user = UserUtils.create();

    user.setDeletedAt(LocalDateTime.now());
    user.setContacts(List.of(ContactUtils.create(user)));

    final var savedUser = userRepository.save(user);
    final var optionalUser = userRepository.findById(savedUser.getId());

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldNotFindUserIfIdlDoesNotExist() {
    final var optionalUser = userRepository.findById(0L);

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldNotFindUserByIdWhenUserNotHaveDefinedContact() {
    final var user = UserUtils.create();
    final var savedUser = userRepository.save(user);
    final var optionalUser = userRepository.findById(savedUser.getId());

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldNotFindUserByIdWhenUserNotHaveStandarnContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    contact.setStandard(false);
    user.setContacts(List.of(contact));

    final var savedUser = userRepository.save(user);
    final var optionalUser = userRepository.findById(savedUser.getId());

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldNotFindUserByIdWhenUserNotHaveVerifiedContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    contact.setVerificationCompletedAt(null);
    user.setContacts(List.of(contact));

    final var savedUser = userRepository.save(user);
    final var optionalUser = userRepository.findById(savedUser.getId());

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldNotFindUserByIdWhenUserContactWasDeleted() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    contact.setDeletedAt(LocalDateTime.now());
    user.setContacts(List.of(contact));

    final var savedUser = userRepository.save(user);
    final var optionalUser = userRepository.findById(savedUser.getId());

    assertTrue(optionalUser.isEmpty());
  }
}
