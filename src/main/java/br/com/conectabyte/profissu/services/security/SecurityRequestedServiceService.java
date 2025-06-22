package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityRequestedServiceService implements OwnerCheck {
  private final RequestedServiceService requestedServiceService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    log.debug("Performing ownership check for requested service ID: {}", id);

    try {
      final var requestedService = requestedServiceService.findById(id);
      final var isOwner = securityService.isOwner(requestedService.getUser().getId());

      log.debug("Ownership check result for requested service ID {}: {}", id, isOwner);
      return isOwner;
    } catch (Exception e) {
      log.debug("Ownership check for requested service ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }

  @Transactional
  public boolean isServiceProvider(Long id) {
    log.debug("Performing service provider check for requested service ID: {}", id);

    try {
      final var requestedService = requestedServiceService.findById(id);
      final var conversation = requestedService.getConversations().stream()
          .filter(c -> c.getOfferStatus() == OfferStatusEnum.ACCEPTED)
          .findFirst()
          .orElseThrow();

      final var isServiceProvider = securityService.isOwner(conversation.getServiceProvider().getId());

      log.debug("Service provider check result for requested service ID {}: {}", id, isServiceProvider);
      return isServiceProvider;
    } catch (Exception e) {
      log.debug("Service provider check for requested service ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }
}
