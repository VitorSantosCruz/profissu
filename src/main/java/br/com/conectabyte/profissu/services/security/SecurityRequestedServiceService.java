package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.RequestedServiceService;
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
}
