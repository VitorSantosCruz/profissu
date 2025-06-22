package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.MessageService;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.MessageUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityMessageService Tests")
class SecurityMessageServiceTest {
  @Mock
  private MessageService messageService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityMessageService securityMessageService;

  private static final Long TEST_MESSAGE_ID = 1L;
  private static final Long SENDER_USER_ID = 10L;
  private static final Long RECEIVER_USER_ID = 20L;
  private static final Long OTHER_USER_ID = 30L;

  @Test
  @DisplayName("Should return true when authenticated user is the message owner")
  void shouldReturnTrueWhenUserIsOwnerOfMessage() {
    User messageOwner = UserUtils.create();
    messageOwner.setId(SENDER_USER_ID);
    Conversation conversation = ConversationUtils.create(null, null, null, null);
    Message message = MessageUtils.create(messageOwner, conversation);

    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenReturn(message);
    when(securityService.isOwner(eq(SENDER_USER_ID))).thenReturn(true);

    boolean isOwner = securityMessageService.ownershipCheck(TEST_MESSAGE_ID);

    assertTrue(isOwner);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, times(1)).isOwner(eq(SENDER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when authenticated user is not the message owner")
  void shouldReturnFalseWhenUserIsNotOwnerOfMessage() {
    User messageOwner = UserUtils.create();
    messageOwner.setId(OTHER_USER_ID);
    Conversation conversation = ConversationUtils.create(null, null, null, null);
    Message message = MessageUtils.create(messageOwner, conversation);

    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenReturn(message);
    when(securityService.isOwner(eq(OTHER_USER_ID))).thenReturn(false);

    boolean isOwner = securityMessageService.ownershipCheck(TEST_MESSAGE_ID);

    assertFalse(isOwner);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, times(1)).isOwner(eq(OTHER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when message not found during ownership check")
  void shouldReturnFalseWhenMessageNotFoundInOwnershipCheck() {
    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenThrow(new ResourceNotFoundException("Message not found"));

    boolean isOwner = securityMessageService.ownershipCheck(TEST_MESSAGE_ID);

    assertFalse(isOwner);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when unexpected exception occurs during ownership check")
  void shouldReturnFalseWhenUnexpectedExceptionInOwnershipCheck() {
    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenThrow(new RuntimeException("Simulated error"));

    boolean isOwner = securityMessageService.ownershipCheck(TEST_MESSAGE_ID);

    assertFalse(isOwner);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return true when authenticated user is the message receiver (not owner)")
  void shouldReturnTrueWhenUserIsTheMessageReceiverAndNotOwner() {
    User messageSender = UserUtils.create();
    messageSender.setId(SENDER_USER_ID);
    User messageReceiver = UserUtils.create();
    messageReceiver.setId(RECEIVER_USER_ID);

    Conversation conversation = ConversationUtils.create(messageSender, messageReceiver, null, null);
    Message message = MessageUtils.create(messageSender, conversation);

    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenReturn(message);
    when(securityService.isOwner(eq(RECEIVER_USER_ID))).thenReturn(false);

    boolean isReceiver = securityMessageService.isMessageReceiver(TEST_MESSAGE_ID);

    assertTrue(isReceiver);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, times(1)).isOwner(eq(RECEIVER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when authenticated user is the message sender (not receiver)")
  void shouldReturnFalseWhenUserIsTheMessageSender() {
    User messageSender = UserUtils.create();
    messageSender.setId(SENDER_USER_ID);
    User messageReceiver = UserUtils.create();
    messageReceiver.setId(RECEIVER_USER_ID);

    Conversation conversation = ConversationUtils.create(messageSender, messageReceiver, null, null);
    Message message = MessageUtils.create(messageSender, conversation);

    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenReturn(message);
    when(securityService.isOwner(eq(RECEIVER_USER_ID))).thenReturn(true);

    boolean isReceiver = securityMessageService.isMessageReceiver(TEST_MESSAGE_ID);

    assertFalse(isReceiver);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, times(1)).isOwner(eq(RECEIVER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when message not found during receiver check")
  void shouldReturnFalseWhenMessageNotFoundInMessageReceiver() {
    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenThrow(new ResourceNotFoundException("Message not found"));

    boolean isReceiver = securityMessageService.isMessageReceiver(TEST_MESSAGE_ID);

    assertFalse(isReceiver);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when unexpected exception occurs during receiver check")
  void shouldReturnFalseWhenUnexpectedExceptionInMessageReceiver() {
    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenThrow(new RuntimeException("Simulated error"));

    boolean isReceiver = securityMessageService.isMessageReceiver(TEST_MESSAGE_ID);

    assertFalse(isReceiver);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should handle case where message owner is the conversation's requester (sender)")
  void shouldHandleMessageOwnerIsRequester() {
    User requester = UserUtils.create();
    requester.setId(SENDER_USER_ID);
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(RECEIVER_USER_ID);

    Conversation conversation = ConversationUtils.create(requester, serviceProvider, null, null);
    Message message = MessageUtils.create(requester, conversation);

    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenReturn(message);
    when(securityService.isOwner(eq(serviceProvider.getId()))).thenReturn(false);

    boolean isReceiver = securityMessageService.isMessageReceiver(TEST_MESSAGE_ID);

    assertTrue(isReceiver);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, times(1)).isOwner(eq(serviceProvider.getId()));
  }

  @Test
  @DisplayName("Should handle case where message owner is the conversation's service provider (sender)")
  void shouldHandleMessageOwnerIsServiceProvider() {
    User requester = UserUtils.create();
    requester.setId(RECEIVER_USER_ID);
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(SENDER_USER_ID);

    Conversation conversation = ConversationUtils.create(requester, serviceProvider, null, null);
    Message message = MessageUtils.create(serviceProvider, conversation);

    when(messageService.findById(eq(TEST_MESSAGE_ID))).thenReturn(message);
    when(securityService.isOwner(eq(requester.getId()))).thenReturn(false);

    boolean isReceiver = securityMessageService.isMessageReceiver(TEST_MESSAGE_ID);

    assertTrue(isReceiver);
    verify(messageService, times(1)).findById(eq(TEST_MESSAGE_ID));
    verify(securityService, times(1)).isOwner(eq(requester.getId()));
  }
}
