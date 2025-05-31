package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class SecurityRequestedServiceServiceTest {
  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityRequestedServiceService securityRequestedServiceService;

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityRequestedServiceService.ownershipCheck(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    when(requestedServiceService.findById(any())).thenReturn(requestedService);

    final var isOwner = securityRequestedServiceService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenRequestedServiceNotFound() {
    when(requestedServiceService.findById(any()))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    final var isOwner = securityRequestedServiceService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserIsServiceProvider() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    final var serviceProvider = UserUtils.create();
    final var acceptedConversation = new Conversation();
    acceptedConversation.setServiceProvider(serviceProvider);
    acceptedConversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    requestedService.setConversations(List.of(acceptedConversation));

    when(requestedServiceService.findById(requestedService.getId())).thenReturn(requestedService);
    when(securityService.isOwner(serviceProvider.getId())).thenReturn(true);

    final var isProvider = securityRequestedServiceService.isServiceProvider(requestedService.getId());

    assertTrue(isProvider);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotServiceProvider() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    final var serviceProvider = UserUtils.create();
    final var acceptedConversation = new Conversation();
    acceptedConversation.setServiceProvider(serviceProvider);
    acceptedConversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    requestedService.setConversations(List.of(acceptedConversation));

    when(requestedServiceService.findById(requestedService.getId())).thenReturn(requestedService);
    when(securityService.isOwner(serviceProvider.getId())).thenReturn(false);

    final var isProvider = securityRequestedServiceService.isServiceProvider(requestedService.getId());

    assertFalse(isProvider);
  }

  @Test
  void shouldReturnFalseWhenNoAcceptedOfferExists() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    final var conversation = new Conversation();
    conversation.setServiceProvider(UserUtils.create());
    conversation.setOfferStatus(OfferStatusEnum.PENDING);

    requestedService.setConversations(List.of(conversation));

    when(requestedServiceService.findById(requestedService.getId())).thenReturn(requestedService);

    final var isProvider = securityRequestedServiceService.isServiceProvider(requestedService.getId());

    assertFalse(isProvider);
  }

  @Test
  void shouldReturnFalseWhenRequestedServiceNotFoundForIsServiceProvider() {
    final Long serviceId = 1L;

    when(requestedServiceService.findById(serviceId))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    final var isProvider = securityRequestedServiceService.isServiceProvider(serviceId);

    assertFalse(isProvider);
  }
}
