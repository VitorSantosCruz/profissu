package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.AddressService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityAddressService implements OwnerCheck {
  private final AddressService addressService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    try {
      final var address = addressService.findById(id);
      return securityService.isOwner(address.getUser().getId());
    } catch (Exception e) {
      return false;
    }
  }
}
