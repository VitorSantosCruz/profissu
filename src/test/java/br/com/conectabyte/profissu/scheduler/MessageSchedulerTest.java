package br.com.conectabyte.profissu.scheduler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.dtos.request.NotificationEmailDto;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.services.MessageService;
import br.com.conectabyte.profissu.services.email.NotificationService;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.MessageUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageScheduler Tests")
class MessageSchedulerTest {

  @Mock
  private MessageService messageService;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private MessageScheduler messageScheduler;

  private User createUserWithContact(String name, String email) {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setName(name);
    contact.setValue(email);
    user.setContacts(List.of(contact));

    return user;
  }

  private User createUserWithNoStandardContact(String name, String email) {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setName(name);
    contact.setValue(email);
    contact.setStandard(false);
    user.setContacts(List.of(contact));

    return user;
  }

  @Test
  @DisplayName("Should send notification for unread messages")
  void shouldSendNotificationForUnreadMessages() {
    final var requester = createUserWithContact("Requester", "requester@conectabyte.com.br");
    final var serviceProvider = createUserWithContact("ServiceProvider", "serviceProvider@conectabyte.com.br");
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    final var unreadMessage = MessageUtils.create(requester, conversation);

    unreadMessage.setCreatedAt(LocalDateTime.now().minusMinutes(10));
    requester.setMessages(List.of(unreadMessage));
    serviceProvider.setMessages(List.of());

    when(messageService.findConversationsWithUnreadMessages(any(LocalDateTime.class)))
        .thenReturn(List.of(conversation));

    messageScheduler.notifyUnreadMessages();

    final var captor = ArgumentCaptor.forClass(NotificationEmailDto.class);

    verify(notificationService, times(1)).send(captor.capture());

    final var sentNotification = captor.getValue();

    assertAll(
        () -> assertEquals("ServiceProvider, Requester sent you a message about Title.",
            sentNotification.notification()),
        () -> assertEquals("serviceProvider@conectabyte.com.br", sentNotification.email()),
        () -> assertTrue(unreadMessage.isNotificationSent()));
  }

  @Test
  @DisplayName("Should not send notification if message does not belong to conversation")
  void shouldNotSendNotificationIfMessageDoesNotBelongToConversation() {
    final var requester = createUserWithContact("Requester", "requester@conectabyte.com.br");
    final var serviceProvider = createUserWithContact("ServiceProvider", "serviceProvider@conectabyte.com.br");
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    final var anotherConversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    conversation.setId(1L);
    anotherConversation.setId(2L);

    final var unreadMessage = MessageUtils.create(requester, anotherConversation);

    requester.setMessages(List.of(unreadMessage));
    serviceProvider.setMessages(List.of());

    when(messageService.findConversationsWithUnreadMessages(any(LocalDateTime.class)))
        .thenReturn(List.of(conversation));

    messageScheduler.notifyUnreadMessages();

    assertAll(
        () -> assertFalse(unreadMessage.isRead()),
        () -> assertFalse(unreadMessage.isNotificationSent()));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should not send notification when all messages are read")
  void shouldNotSendNotificationWhenAllMessagesAreRead() {
    final var requester = createUserWithContact("Requester", "requester@conectabyte.com.br");
    final var serviceProvider = createUserWithContact("ServiceProvider", "serviceProvider@conectabyte.com.br");
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    final var readMessage = MessageUtils.create(requester, conversation);

    readMessage.setRead(true);
    readMessage.setCreatedAt(LocalDateTime.now().minusMinutes(10));
    requester.setMessages(List.of(readMessage));
    serviceProvider.setMessages(List.of());

    when(messageService.findConversationsWithUnreadMessages(any(LocalDateTime.class)))
        .thenReturn(List.of(conversation));

    messageScheduler.notifyUnreadMessages();

    assertAll(
        () -> assertTrue(readMessage.isRead()),
        () -> assertFalse(readMessage.isNotificationSent()));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should not send notification if already notified")
  void shouldNotSendNotificationIfAlreadyNotified() {
    final var requester = createUserWithContact("Requester", "requester@conectabyte.com.br");
    final var serviceProvider = createUserWithContact("ServiceProvider", "serviceProvider@conectabyte.com.br");
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    final var unreadMessage = MessageUtils.create(requester, conversation);

    unreadMessage.setNotificationSent(true);
    unreadMessage.setCreatedAt(LocalDateTime.now().minusMinutes(10));
    requester.setMessages(List.of(unreadMessage));
    serviceProvider.setMessages(List.of());

    when(messageService.findConversationsWithUnreadMessages(any(LocalDateTime.class)))
        .thenReturn(List.of(conversation));

    messageScheduler.notifyUnreadMessages();

    assertAll(
        () -> assertFalse(unreadMessage.isRead()),
        () -> assertTrue(unreadMessage.isNotificationSent()));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should not send notification if no conversations with unread messages are found")
  void shouldNotSendNotificationIfNoConversationsFound() {
    when(messageService.findConversationsWithUnreadMessages(any(LocalDateTime.class))).thenReturn(List.of());

    messageScheduler.notifyUnreadMessages();

    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should not send notification if receiver has no standard contact")
  void shouldNotSendNotificationIfReceiverHasNoStandardContact() {
    final var requester = createUserWithContact("Requester", "requester@conectabyte.com.br");
    final var serviceProvider = createUserWithNoStandardContact("ServiceProvider",
        "serviceProvider@conectabyte.com.br");
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    final var unreadMessage = MessageUtils.create(requester, conversation);

    unreadMessage.setCreatedAt(LocalDateTime.now().minusMinutes(10));
    requester.setMessages(List.of(unreadMessage));
    serviceProvider.setMessages(List.of());

    when(messageService.findConversationsWithUnreadMessages(any(LocalDateTime.class)))
        .thenReturn(List.of(conversation));

    messageScheduler.notifyUnreadMessages();

    verify(notificationService, never()).send(any());
  }
}
