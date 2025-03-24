package br.com.conectabyte.profissu.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RequestedServiceRepositoryTest {
  @Autowired
  private RequestedServiceRepository requestedServiceRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  public void shouldReturnRequestedServicePageWhenHavePandingRequestServices() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);
    final var requestedService = RequestedServiceUtils.create(savedUser, savedUser.getAddresses().get(0));

    requestedServiceRepository.save(requestedService);

    final var requestedServicePage = requestedServiceRepository.findAvailableServiceRequests(Pageable.ofSize(10));

    assertTrue(requestedServicePage.getTotalElements() > 0);
  }

  @Test
  public void shouldReturnVoidPageWhenNotHavePandingRequestServices() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);
    final var requestedService = RequestedServiceUtils.create(savedUser, savedUser.getAddresses().get(0));

    requestedService.setStatus(RequestedServiceStatusEnum.DONE);
    requestedServiceRepository.save(requestedService);

    final var requestedServicePage = requestedServiceRepository.findAvailableServiceRequests(Pageable.ofSize(10));

    assertEquals(requestedServicePage.getTotalElements(), 0);
  }

  @Test
  public void shouldReturnVoidPageWhenRequestServiceIsDeleted() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);
    final var requestedService = RequestedServiceUtils.create(savedUser, savedUser.getAddresses().get(0));

    requestedService.setDeletedAt(LocalDateTime.now());
    requestedServiceRepository.save(requestedService);

    final var requestedServicePage = requestedServiceRepository.findAvailableServiceRequests(Pageable.ofSize(10));

    assertEquals(requestedServicePage.getTotalElements(), 0);
  }
}
