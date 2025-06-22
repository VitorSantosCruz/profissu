package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityConversationService implements OwnerCheck {
  private final ConversationService conversationService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    log.debug("Performing ownership check for conversation ID: {}", id);

    try {
      final var conversation = conversationService.findById(id);
      final var isOwner = securityService.isOwner(conversation.getServiceProvider().getId());

      log.debug("Ownership check result for conversation ID {} (service provider): {}", id, isOwner);
      return isOwner;
    } catch (Exception e) {
      log.debug("Ownership check for conversation ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }

  public boolean isRequestedServiceOwner(Long id) {
    log.debug("Performing requested service ownership check for conversation ID: {}", id);

    try {
      final var conversation = conversationService.findById(id);
      final var isOwner = securityService.isOwner(conversation.getRequester().getId());

      log.debug("Requested service ownership check result for conversation ID {} (requester): {}", id, isOwner);
      return isOwner;
    } catch (Exception e) {
      log.debug("Requested service ownership check for conversation ID {} failed due to exception: {}", id,
          e.getMessage());
      return false;
    }
  }
}
