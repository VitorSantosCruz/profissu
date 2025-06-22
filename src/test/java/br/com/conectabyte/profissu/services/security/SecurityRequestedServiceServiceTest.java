package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.entities.Address;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityRequestedServiceService Tests")
public class SecurityRequestedServiceServiceTest {
  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityRequestedServiceService securityRequestedServiceService;

  private static final Long TEST_REQUESTED_SERVICE_ID = 1L;
  private static final Long AUTHENTICATED_USER_ID = 10L;
  private static final Long OTHER_USER_ID = 20L;
  private static final Long SERVICE_PROVIDER_USER_ID = 30L;

  @Test
  @DisplayName("Should return true when authenticated user is owner of requested service")
  void shouldReturnTrueWhenUserIsOwnerOfRequestedService() {
    User ownerUser = UserUtils.create();
    ownerUser.setId(AUTHENTICATED_USER_ID);
    Address address = AddressUtils.create(ownerUser);
    RequestedService requestedService = RequestedServiceUtils.create(ownerUser, address, List.of());

    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID))).thenReturn(requestedService);
    when(securityService.isOwner(eq(AUTHENTICATED_USER_ID))).thenReturn(true);

    boolean isOwner = securityRequestedServiceService.ownershipCheck(TEST_REQUESTED_SERVICE_ID);

    assertTrue(isOwner);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, times(1)).isOwner(eq(AUTHENTICATED_USER_ID));
  }

  @Test
  @DisplayName("Should return false when authenticated user is not owner of requested service")
  void shouldReturnFalseWhenUserIsNotOwnerOfRequestedService() {
    User serviceOwner = UserUtils.create();
    serviceOwner.setId(OTHER_USER_ID);
    Address address = AddressUtils.create(serviceOwner);
    RequestedService requestedService = RequestedServiceUtils.create(serviceOwner, address, List.of());

    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID))).thenReturn(requestedService);
    when(securityService.isOwner(eq(OTHER_USER_ID))).thenReturn(false);

    boolean isOwner = securityRequestedServiceService.ownershipCheck(TEST_REQUESTED_SERVICE_ID);

    assertFalse(isOwner);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, times(1)).isOwner(eq(OTHER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when requested service not found for ownership check")
  void shouldReturnFalseWhenRequestedServiceNotFoundForOwnershipCheck() {
    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID)))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    boolean isOwner = securityRequestedServiceService.ownershipCheck(TEST_REQUESTED_SERVICE_ID);

    assertFalse(isOwner);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when an unexpected exception occurs during ownership check")
  void shouldReturnFalseWhenUnexpectedExceptionInOwnershipCheck() {
    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID)))
        .thenThrow(new RuntimeException("Simulated error"));

    boolean isOwner = securityRequestedServiceService.ownershipCheck(TEST_REQUESTED_SERVICE_ID);

    assertFalse(isOwner);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return true when authenticated user is the service provider for accepted offer")
  void shouldReturnTrueWhenUserIsServiceProvider() {
    User requester = UserUtils.create();
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(AUTHENTICATED_USER_ID);

    Conversation acceptedConversation = new Conversation();
    acceptedConversation.setServiceProvider(serviceProvider);
    acceptedConversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    RequestedService requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester),
        List.of(acceptedConversation));

    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID))).thenReturn(requestedService);
    when(securityService.isOwner(eq(AUTHENTICATED_USER_ID))).thenReturn(true);

    boolean isProvider = securityRequestedServiceService.isServiceProvider(TEST_REQUESTED_SERVICE_ID);

    assertTrue(isProvider);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, times(1)).isOwner(eq(AUTHENTICATED_USER_ID));
  }

  @Test
  @DisplayName("Should return false when authenticated user is not the service provider for accepted offer")
  void shouldReturnFalseWhenUserIsNotServiceProvider() {
    User requester = UserUtils.create();
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(SERVICE_PROVIDER_USER_ID);

    Conversation acceptedConversation = new Conversation();
    acceptedConversation.setServiceProvider(serviceProvider);
    acceptedConversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    RequestedService requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester),
        List.of(acceptedConversation));

    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID))).thenReturn(requestedService);
    when(securityService.isOwner(eq(SERVICE_PROVIDER_USER_ID))).thenReturn(false);

    boolean isProvider = securityRequestedServiceService.isServiceProvider(TEST_REQUESTED_SERVICE_ID);

    assertFalse(isProvider);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, times(1)).isOwner(eq(SERVICE_PROVIDER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when no accepted offer exists for service provider check")
  void shouldReturnFalseWhenNoAcceptedOfferExists() {
    User requester = UserUtils.create();
    User serviceProvider = UserUtils.create();

    Conversation pendingConversation = new Conversation();
    pendingConversation.setServiceProvider(serviceProvider);
    pendingConversation.setOfferStatus(OfferStatusEnum.PENDING);

    RequestedService requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester),
        List.of(pendingConversation));

    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID))).thenReturn(requestedService);

    boolean isProvider = securityRequestedServiceService.isServiceProvider(TEST_REQUESTED_SERVICE_ID);

    assertFalse(isProvider);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when requested service not found for service provider check")
  void shouldReturnFalseWhenRequestedServiceNotFoundForIsServiceProvider() {
    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID)))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    boolean isProvider = securityRequestedServiceService.isServiceProvider(TEST_REQUESTED_SERVICE_ID);

    assertFalse(isProvider);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when an unexpected exception occurs during service provider check")
  void shouldReturnFalseWhenUnexpectedExceptionInIsServiceProvider() {
    when(requestedServiceService.findById(eq(TEST_REQUESTED_SERVICE_ID)))
        .thenThrow(new RuntimeException("Simulated error"));

    boolean isProvider = securityRequestedServiceService.isServiceProvider(TEST_REQUESTED_SERVICE_ID);

    assertFalse(isProvider);
    verify(requestedServiceService, times(1)).findById(eq(TEST_REQUESTED_SERVICE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }
}
