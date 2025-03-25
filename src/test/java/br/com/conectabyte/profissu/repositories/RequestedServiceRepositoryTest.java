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

import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
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

  @Autowired
  private ConversationRepository conversationRepository;

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

  @Test
  public void shouldFindRequestedServicesByUser() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);
    final var requestedService = RequestedServiceUtils.create(savedUser, savedUser.getAddresses().get(0));

    requestedServiceRepository.save(requestedService);

    final var requestedServicePage = requestedServiceRepository.findByUserId(savedUser.getId(), Pageable.ofSize(10));

    assertEquals(1, requestedServicePage.getTotalElements());
    assertEquals(savedUser.getId(), requestedServicePage.getContent().get(0).getUser().getId());
  }

  @Test
  public void shouldFindRequestedServicesByServiceProviderInAcceptedConversation() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(savedUser, savedUser.getAddresses().get(0));
    final var savedServiceProvider = userRepository.save(serviceProvider);
    final var savedRequestedService = requestedServiceRepository.save(requestedService);
    final var conversation = ConversationUtils.create(savedUser, savedServiceProvider, savedRequestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);
    conversationRepository.save(conversation);

    final var requestedServicePage = requestedServiceRepository.findByUserId(savedServiceProvider.getId(), Pageable.ofSize(10));

    assertEquals(1, requestedServicePage.getTotalElements());
    assertEquals(savedRequestedService.getId(), requestedServicePage.getContent().get(0).getId());
  }

  @Test
  public void shouldNotFindRequestedServicesByServiceProviderInPendingConversation() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(savedUser, savedUser.getAddresses().get(0));
    final var savedServiceProvider = userRepository.save(serviceProvider);
    final var savedRequestedService = requestedServiceRepository.save(requestedService);
    final var conversation = ConversationUtils.create(savedUser, savedServiceProvider, savedRequestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.PENDING);
    conversationRepository.save(conversation);

    final var requestedServicePage = requestedServiceRepository.findByUserId(savedServiceProvider.getId(), Pageable.ofSize(10));

    assertEquals(0, requestedServicePage.getTotalElements());
  }

  @Test
  public void shouldNotFindDeletedRequestedServices() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setAddresses(List.of(address));

    final var savedUser = userRepository.save(user);
    final var requestedService = RequestedServiceUtils.create(savedUser, savedUser.getAddresses().get(0));

    requestedService.setDeletedAt(LocalDateTime.now());
    requestedServiceRepository.save(requestedService);

    final var result = requestedServiceRepository.findByUserId(user.getId(), Pageable.ofSize(10));

    assertEquals(0, result.getTotalElements());
  }
}
