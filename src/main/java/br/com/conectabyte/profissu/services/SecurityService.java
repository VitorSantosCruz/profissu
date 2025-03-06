package br.com.conectabyte.profissu.services;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.enums.RoleEnum;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityService {
  private final JwtService jwtService;
  private final ContactService contactService;
  private final AddressService addressService;

  public boolean isOwner(Long userId) {
    return this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()).equals(userId))
        .orElse(false);
  }

  public boolean isOwnerOfContact(Long id) {
    try {
      final var contact = contactService.findById(id);
      return isOwner(contact.getUser().getId());
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isOwnerOfAddress(Long id) {
    try {
      final var address = addressService.findById(id);
      return isOwner(address.getUser().getId());
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isAdmin() {
    return this.jwtService.getClaims()
        .map(claims -> String.valueOf(claims.get("ROLE")).contains(RoleEnum.ADMIN.name()))
        .orElse(false);
  }
}
