package br.com.conectabyte.profissu.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.services.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageScheduler {
  private final MessageService messageService;

  @Scheduled(initialDelay = 0, fixedRate = 600000)
  @Transactional
  public void teste() {
    final var thresholdDate = LocalDateTime.now().minusMinutes(5);
    final var conversationsWithUnreadMessages = messageService.findConversationsWithUnreadMessages(thresholdDate);

    conversationsWithUnreadMessages.forEach(c -> {
      final var requester = c.getRequestedService().getUser();
      final var serviceProvider = c.getServiceProvider();
      final var requesterMessages = requester.getMessages();
      final var serviceProviderMessages = serviceProvider.getMessages();

      proprocessMessages(requesterMessages, c, serviceProvider, thresholdDate);
      proprocessMessages(serviceProviderMessages, c, requester, thresholdDate);
    });
  }

  private void proprocessMessages(List<Message> messages, Conversation conversation, User reciver,
      LocalDateTime thresholdDate) {
    final var messagesReaded = messages.stream()
        .filter(m -> conversation.getId() == m.getConversation().getId())
        .filter(m -> !m.isRead())
        .filter(m -> m.getCreatedAt().isBefore(thresholdDate))
        .map(m -> {
          m.setRead(true);
          return m;
        })
        .collect(Collectors.toList());

    System.out.println(messagesReaded.size() > 0 ? reciver.getName() + ", " + messages.get(0).getUser().getName()
        + " te enviou uma mensagem sobre " + conversation.getRequestedService().getTitle() : "Olá");
  }
}
