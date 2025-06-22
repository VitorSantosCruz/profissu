package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAddressService implements OwnerCheck {
  private final AddressService addressService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    log.debug("Performing ownership check for address ID: {}", id);

    try {
      final var address = addressService.findById(id);
      final var isOwner = securityService.isOwner(address.getUser().getId());

      log.debug("Ownership check result for address ID {}: {}", id, isOwner);
      return isOwner;
    } catch (Exception e) {
      log.debug("Ownership check for address ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }
}
