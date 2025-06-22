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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageScheduler {
  private final MessageService messageService;
  private final NotificationService notificationService;

  @Scheduled(initialDelay = 0, fixedRate = 600000)
  @Transactional
  public void notifyUnreadMessages() {
    log.info("Starting scheduled task: notifyUnreadMessages at {}", LocalDateTime.now());

    final var thresholdDate = LocalDateTime.now().minusMinutes(5);
    log.debug("Threshold date for unread messages: {}", thresholdDate);

    final var conversationsWithUnreadMessages = messageService.findConversationsWithUnreadMessages(thresholdDate);
    log.debug("Found {} conversations with potential unread messages.", conversationsWithUnreadMessages.size());

    if (conversationsWithUnreadMessages.isEmpty()) {
      log.info("No conversations with unread messages requiring notification found.");
    } else {
      conversationsWithUnreadMessages.forEach(c -> {
        log.debug("Processing conversation ID: {}", c.getId());
        final var requester = c.getRequester();
        final var serviceProvider = c.getServiceProvider();

        proprocessMessages(requester.getMessages(), c, serviceProvider, thresholdDate);
        proprocessMessages(serviceProvider.getMessages(), c, requester, thresholdDate);
      });
      log.info("Finished processing unread messages notifications.");
    }
  }

  private void proprocessMessages(List<Message> messages, Conversation conversation, User receiver,
      LocalDateTime thresholdDate) {
    log.debug("Preprocessing messages for receiver: {} in conversation: {}", receiver.getId(), conversation.getId());

    final var messagesToNotify = messages.stream()
        .filter(m -> m.getConversation().getId() == conversation.getId())
        .filter(m -> !m.isRead())
        .filter(m -> !m.isNotificationSent())
        .filter(m -> m.getCreatedAt().isBefore(thresholdDate))
        .map(m -> {
          m.setNotificationSent(true);
          return m;
        })
        .collect(Collectors.toList());

    if (!messagesToNotify.isEmpty()) {
      log.info("Found {} unread messages for user {} in conversation {}. Sending notifications...",
          messagesToNotify.size(), receiver.getName(), conversation.getId());

      receiver.getContacts().stream()
          .filter(Contact::isStandard)
          .forEach(contact -> {
            final var notification = String.format(
                "%s, %s sent you a message about %s.",
                receiver.getName(),
                messagesToNotify.get(0).getUser().getName(),
                conversation.getRequestedService().getTitle());

            log.debug("Sending notification email to {} for conversation {}. Message: {}", contact.getValue(),
                conversation.getId(), notification);
            notificationService.send(new NotificationEmailDto(notification, contact.getValue()));
          });
    } else {
      log.debug("No new unread messages requiring notification for user {} in conversation {}.", receiver.getName(),
          conversation.getId());
    }
  }
}
