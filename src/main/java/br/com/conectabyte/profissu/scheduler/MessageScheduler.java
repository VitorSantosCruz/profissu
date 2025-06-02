package br.com.conectabyte.profissu.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.NotificationEmailDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.services.MessageService;
import br.com.conectabyte.profissu.services.email.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageScheduler {
  private final MessageService messageService;
  private final NotificationService notificationService;

  @Scheduled(initialDelay = 0, fixedRate = 600000)
  @Transactional
  public void notifyUnreadMessages() {
    final var thresholdDate = LocalDateTime.now().minusMinutes(5);
    final var conversationsWithUnreadMessages = messageService.findConversationsWithUnreadMessages(thresholdDate);

    conversationsWithUnreadMessages.forEach(c -> {
      final var requester = c.getRequester();
      final var serviceProvider = c.getServiceProvider();
      final var requesterMessages = requester.getMessages();
      final var serviceProviderMessages = serviceProvider.getMessages();

      proprocessMessages(requesterMessages, c, serviceProvider, thresholdDate);
      proprocessMessages(serviceProviderMessages, c, requester, thresholdDate);
    });
  }

  private void proprocessMessages(List<Message> messages, Conversation conversation, User receiver,
      LocalDateTime thresholdDate) {
    final var messagesReaded = messages.stream()
        .filter(m -> m.getConversation().getId() == conversation.getId())
        .filter(m -> !m.isRead())
        .filter(m -> !m.isNotificationSent())
        .filter(m -> m.getCreatedAt().isBefore(thresholdDate))
        .map(m -> {
          m.setNotificationSent(true);
          return m;
        })
        .collect(Collectors.toList());

    if (messagesReaded.size() > 0) {
      receiver.getContacts().stream()
          .filter(Contact::isStandard)
          .forEach(contact -> {
            final var notification = String.format(
                "%s, %s sent you a message about %s.",
                receiver.getName(),
                messages.get(0).getUser().getName(),
                conversation.getRequestedService().getTitle());

            notificationService.send(new NotificationEmailDto(notification, contact.getValue()));
          });
    }
  }
}
