package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.MessageService;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.MessageUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class SecurityMessageServiceTest {

  @Mock
  private MessageService messageService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityMessageService securityMessageService;

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfServiceProvider() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var conversation = ConversationUtils.create(null, user, null, null);
    final var message = MessageUtils.create(null, conversation);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityMessageService.ownershipCheck(1L);

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfServiceProvider() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var conversation = ConversationUtils.create(user, null, null, null);
    final var message = MessageUtils.create(null, conversation);

    when(messageService.findById(any())).thenReturn(message);

    final var isOwner = securityMessageService.ownershipCheck(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenMessageNotFoundInOwnershipCheck() {
    when(messageService.findById(any())).thenThrow(new ResourceNotFoundException("Message not found"));

    final var isOwner = securityMessageService.ownershipCheck(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfRequestedService() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var conversation = ConversationUtils.create(user, null, null, null);
    final var message = MessageUtils.create(null, conversation);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityMessageService.requestedServiceOwner(1L);

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfRequestedService() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var conversation = ConversationUtils.create(user, null, null, null);
    final var message = MessageUtils.create(null, conversation);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(false);

    final var isOwner = securityMessageService.requestedServiceOwner(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenMessageNotFoundInRequestedServiceOwner() {
    when(messageService.findById(any())).thenThrow(new ResourceNotFoundException("Message not found"));

    final var isOwner = securityMessageService.requestedServiceOwner(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserIsNotTheMessageReceiver() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var message = MessageUtils.create(user, null);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(false);

    final var isReceiver = securityMessageService.messageReciver(1L);

    assertTrue(isReceiver);
  }

  @Test
  void shouldReturnFalseWhenUserIsTheMessageReceiver() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var message = MessageUtils.create(user, null);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isReceiver = securityMessageService.messageReciver(1L);

    assertFalse(isReceiver);
  }

  @Test
  void shouldReturnFalseWhenMessageNotFoundInMessageReceiver() {
    when(messageService.findById(any())).thenThrow(new ResourceNotFoundException("Message not found"));

    final var isReceiver = securityMessageService.messageReciver(1L);

    assertFalse(isReceiver);
  }
}
