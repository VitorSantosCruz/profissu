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
  void shouldReturnTrueWhenUserIsOwnerOfMessage() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var conversation = ConversationUtils.create(null, null, null, null);
    final var message = MessageUtils.create(user, conversation);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityMessageService.ownershipCheck(1L);

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfMessage() {
    final var user = UserUtils.create();

    user.setId(1L);

    final var conversation = ConversationUtils.create(null, null, null, null);
    final var message = MessageUtils.create(user, conversation);

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
  void shouldReturnTrueWhenUserIsNotTheMessageOwner() {
    final var messageOwner = UserUtils.create();
    final var messageReciver = UserUtils.create();

    messageOwner.setId(1L);
    messageReciver.setId(2L);

    final var conversation = ConversationUtils.create(messageOwner, messageReciver, null, null);
    final var message = MessageUtils.create(messageOwner, conversation);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(false);

    final var isReceiver = securityMessageService.isMessageReceiver(1L);

    assertTrue(isReceiver);
  }

  @Test
  void shouldReturnFalseWhenUserIsTheMessageReceiver() {
    final var messageOwner = UserUtils.create();
    final var messageReciver = UserUtils.create();

    messageOwner.setId(1L);
    messageReciver.setId(2L);

    final var conversation = ConversationUtils.create(messageOwner, messageReciver, null, null);
    final var message = MessageUtils.create(messageOwner, conversation);

    when(messageService.findById(any())).thenReturn(message);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isReceiver = securityMessageService.isMessageReceiver(1L);

    assertFalse(isReceiver);
  }

  @Test
  void shouldReturnFalseWhenMessageNotFoundInMessageReceiver() {
    when(messageService.findById(any())).thenThrow(new ResourceNotFoundException("Message not found"));

    final var isReceiver = securityMessageService.isMessageReceiver(1L);

    assertFalse(isReceiver);
  }
}
