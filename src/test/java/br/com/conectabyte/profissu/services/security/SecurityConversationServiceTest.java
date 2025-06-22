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

import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.ConversationService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConversationService Tests")
public class SecurityConversationServiceTest {
  @Mock
  private ConversationService conversationService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityConversationService securityConversationService;

  private static final Long TEST_CONVERSATION_ID = 1L;
  private static final Long AUTHENTICATED_USER_ID = 10L;
  private static final Long OTHER_USER_ID = 20L;

  @Test
  @DisplayName("Should return true when authenticated user is service provider owner of conversation")
  void shouldReturnTrueWhenUserIsServiceProviderOwnerOfConversation() {
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(AUTHENTICATED_USER_ID);
    User requester = UserUtils.create();
    RequestedService requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester),
        List.of());
    Conversation conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    when(conversationService.findById(eq(TEST_CONVERSATION_ID))).thenReturn(conversation);
    when(securityService.isOwner(eq(AUTHENTICATED_USER_ID))).thenReturn(true);

    boolean isOwner = securityConversationService.ownershipCheck(TEST_CONVERSATION_ID);

    assertTrue(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, times(1)).isOwner(eq(AUTHENTICATED_USER_ID));
  }

  @Test
  @DisplayName("Should return false when authenticated user is not service provider owner of conversation")
  void shouldReturnFalseWhenUserIsNotServiceProviderOwnerOfConversation() {
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(OTHER_USER_ID);
    User requester = UserUtils.create();
    RequestedService requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester),
        List.of());
    Conversation conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    when(conversationService.findById(eq(TEST_CONVERSATION_ID))).thenReturn(conversation);
    when(securityService.isOwner(eq(OTHER_USER_ID))).thenReturn(false);

    boolean isOwner = securityConversationService.ownershipCheck(TEST_CONVERSATION_ID);

    assertFalse(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, times(1)).isOwner(eq(OTHER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when conversation not found for service provider ownership check")
  void shouldReturnFalseWhenConversationNotFoundForServiceProviderCheck() {
    when(conversationService.findById(eq(TEST_CONVERSATION_ID)))
        .thenThrow(new ResourceNotFoundException("Conversation not found"));

    boolean isOwner = securityConversationService.ownershipCheck(TEST_CONVERSATION_ID);

    assertFalse(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when unexpected exception occurs for service provider ownership check")
  void shouldReturnFalseWhenUnexpectedExceptionForServiceProviderCheck() {
    when(conversationService.findById(eq(TEST_CONVERSATION_ID))).thenThrow(new RuntimeException("Simulated error"));

    boolean isOwner = securityConversationService.ownershipCheck(TEST_CONVERSATION_ID);

    assertFalse(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return true when authenticated user is requester owner of requested service in conversation")
  void shouldReturnTrueWhenUserIsRequesterOwnerOfRequestedService() {
    User requester = UserUtils.create();
    requester.setId(AUTHENTICATED_USER_ID);
    User serviceProvider = UserUtils.create();
    RequestedService requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester),
        List.of());
    Conversation conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    when(conversationService.findById(eq(TEST_CONVERSATION_ID))).thenReturn(conversation);
    when(securityService.isOwner(eq(AUTHENTICATED_USER_ID))).thenReturn(true);

    boolean isOwner = securityConversationService.isRequestedServiceOwner(TEST_CONVERSATION_ID);

    assertTrue(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, times(1)).isOwner(eq(AUTHENTICATED_USER_ID));
  }

  @Test
  @DisplayName("Should return false when authenticated user is not requester owner of requested service in conversation")
  void shouldReturnFalseWhenUserIsNotRequesterOwnerOfRequestedService() {
    User requester = UserUtils.create();
    requester.setId(OTHER_USER_ID);
    User serviceProvider = UserUtils.create();
    RequestedService requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester),
        List.of());
    Conversation conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    when(conversationService.findById(eq(TEST_CONVERSATION_ID))).thenReturn(conversation);
    when(securityService.isOwner(eq(OTHER_USER_ID))).thenReturn(false);

    boolean isOwner = securityConversationService.isRequestedServiceOwner(TEST_CONVERSATION_ID);

    assertFalse(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, times(1)).isOwner(eq(OTHER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when conversation not found for requested service ownership check")
  void shouldReturnFalseWhenConversationNotFoundForRequestedServiceCheck() {
    when(conversationService.findById(eq(TEST_CONVERSATION_ID)))
        .thenThrow(new ResourceNotFoundException("Conversation not found"));

    boolean isOwner = securityConversationService.isRequestedServiceOwner(TEST_CONVERSATION_ID);

    assertFalse(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when unexpected exception occurs for requested service ownership check")
  void shouldReturnFalseWhenUnexpectedExceptionForRequestedServiceCheck() {
    when(conversationService.findById(eq(TEST_CONVERSATION_ID))).thenThrow(new RuntimeException("Simulated error"));

    boolean isOwner = securityConversationService.isRequestedServiceOwner(TEST_CONVERSATION_ID);

    assertFalse(isOwner);
    verify(conversationService, times(1)).findById(eq(TEST_CONVERSATION_ID));
    verify(securityService, never()).isOwner(anyLong());
  }
}
