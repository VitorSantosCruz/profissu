package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.MessageService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityMessageService implements OwnerCheck {
  private final MessageService messageService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    try {
      final var message = messageService.findById(id);

      return securityService.isOwner(message.getUser().getId());
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isMessageReceiver(Long id) {
    try {
      final var message = messageService.findById(id);

      var messageReceiverId = message.getConversation().getRequester().getId();

      if (messageReceiverId == message.getUser().getId()) {
        messageReceiverId = message.getConversation().getServiceProvider().getId();
      }

      return !securityService.isOwner(messageReceiverId);
    } catch (Exception e) {
      return false;
    }
  }
}
