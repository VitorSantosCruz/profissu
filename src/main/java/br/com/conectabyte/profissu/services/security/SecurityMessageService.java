package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMessageService implements OwnerCheck {
  private final MessageService messageService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    log.debug("Performing ownership check for message ID: {}", id);

    try {
      final var message = messageService.findById(id);
      final var isOwner = securityService.isOwner(message.getUser().getId());

      log.debug("Ownership check result for message ID {}: {}", id, isOwner);
      return isOwner;
    } catch (Exception e) {
      log.debug("Ownership check for message ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }

  public boolean isMessageReceiver(Long id) {
    log.debug("Performing message receiver check for message ID: {}", id);

    try {
      final var message = messageService.findById(id);

      var messageReceiverId = message.getConversation().getRequester().getId();

      if (messageReceiverId == message.getUser().getId()) {
        messageReceiverId = message.getConversation().getServiceProvider().getId();
      }

      final var isReceiver = !securityService.isOwner(messageReceiverId);

      log.debug("Message receiver check result for message ID {}: {}", id, isReceiver);
      return isReceiver;
    } catch (Exception e) {
      log.debug("Message receiver check for message ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }
}
