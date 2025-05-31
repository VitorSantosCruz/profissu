package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityRequestedServiceService implements OwnerCheck {
  private final RequestedServiceService requestedServiceService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    try {
      final var requestedService = requestedServiceService.findById(id);

      return securityService.isOwner(requestedService.getUser().getId());
    } catch (Exception e) {
      return false;
    }
  }

  @Transactional
  public boolean isServiceProvider(Long id) {
    try {
      final var requestedService = requestedServiceService.findById(id);
      final var conversation = requestedService.getConversations().stream()
          .filter(c -> c.getOfferStatus() == OfferStatusEnum.ACCEPTED)
          .findFirst()
          .orElseThrow();

      return securityService.isOwner(conversation.getServiceProvider().getId());
    } catch (Exception e) {
      return false;
    }
  }
}
