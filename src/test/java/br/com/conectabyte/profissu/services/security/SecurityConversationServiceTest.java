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

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.ConversationService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class SecurityConversationServiceTest {
  @Mock
  private ConversationService conversationService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityConversationService securityConversationService;

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfConversation() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    when(conversationService.findById(any())).thenReturn(conversation);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityConversationService.ownershipCheck(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfConversation() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    when(conversationService.findById(any())).thenReturn(conversation);

    final var isOwner = securityConversationService.ownershipCheck(0L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenConversationNotFound() {
    when(conversationService.findById(any())).thenThrow(new ResourceNotFoundException("Conversation not found"));

    final var isOwner = securityConversationService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    when(conversationService.findById(any())).thenReturn(conversation);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityConversationService.requestedServiceOwner(conversation.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    when(conversationService.findById(any())).thenReturn(conversation);

    final var isOwner = securityConversationService.requestedServiceOwner(0L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenConversationNotFounda() {
    when(conversationService.findById(any())).thenThrow(new ResourceNotFoundException("Conversation not found"));

    final var isOwner = securityConversationService.requestedServiceOwner(any());

    assertFalse(isOwner);
  }
}
