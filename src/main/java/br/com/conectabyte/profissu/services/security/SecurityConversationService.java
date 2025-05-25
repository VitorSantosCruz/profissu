package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.ConversationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityConversationService implements OwnerCheck {
  private final ConversationService conversationService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    try {
      final var conversation = conversationService.findById(id);

      return securityService.isOwner(conversation.getServiceProvider().getId());
    } catch (Exception e) {
      return false;
    }
  }

  public boolean requestedServiceOwner(Long id) {
    try {
      final var conversation = conversationService.findById(id);

      return securityService.isOwner(conversation.getRequester().getId());
    } catch (Exception e) {
      return false;
    }
  }
}
