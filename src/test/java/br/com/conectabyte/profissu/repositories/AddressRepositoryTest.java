package br.com.conectabyte.profissu.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AddressRepositoryTest {
  @Autowired
  private AddressRepository addressRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  public void shouldReturnAddressByIdWhenIsNotDeletedAndExists() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    address.setId(1L);

    user.setAddresses(List.of(address));
    userRepository.save(user);

    final var findedAddress = addressRepository.findById(1L);

    assertTrue(findedAddress.isPresent());
  }

  @Test
  public void shouldReturnEmptyWhenAddressIsDeleted() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    address.setId(1L);

    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);

    savedUser.getAddresses().get(0).setDeletedAt(LocalDateTime.now());

    final var findedAddress = addressRepository.findById(1L);

    assertTrue(findedAddress.isEmpty());
  }

  @Test
  public void shouldReturnEmptyWhenAddressNotFound() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    address.setId(1L);

    user.setAddresses(List.of(address));
    userRepository.save(user);

    final var findedAddress = addressRepository.findById(0L);

    assertTrue(findedAddress.isEmpty());
  }
}
