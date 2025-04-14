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
public class ContactRepositoryTest {
  @Autowired
  private ContactRepository contactRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  public void shouldReturnContactByIdWhenIsNotDeletedAndExists() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));
    userRepository.save(user);

    final var findedContact = contactRepository.findById(1L);

    assertTrue(findedContact.isPresent());
  }

  @Test
  public void shouldReturnEmptyByIdWhenContactIsDeleted() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));

    final var savedUser = userRepository.save(user);
    final var savedContact = savedUser.getContacts().get(0);

    savedContact.setDeletedAt(LocalDateTime.now());

    final var findedContact = contactRepository.findById(savedContact.getId());

    assertTrue(findedContact.isEmpty());
  }

  @Test
  public void shouldReturnEmptyByIdWhenContactNotFound() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));
    userRepository.save(user);

    final var findedContact = contactRepository.findById(0L);

    assertTrue(findedContact.isEmpty());
  }

  @Test
  public void shouldReturnContactByValueWhenIsNotDeletedAndExists() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));
    userRepository.save(user);

    final var findedContact = contactRepository.findByValue(contact.getValue());

    assertTrue(findedContact.isPresent());
  }

  @Test
  public void shouldReturnEmptyByValueWhenContactIsDeleted() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));

    final var savedUser = userRepository.save(user);

    savedUser.getContacts().get(0).setDeletedAt(LocalDateTime.now());

    final var findedContact = contactRepository.findByValue(contact.getValue());

    assertTrue(findedContact.isEmpty());
  }

  @Test
  public void shouldReturnEmptyByValueWhenContactNotFound() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setContacts(List.of(contact));
    userRepository.save(user);

    final var findedContact = contactRepository.findByValue("invalid@conectabyte.com.br");

    assertTrue(findedContact.isEmpty());
  }
}
