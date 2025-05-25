package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.ContactService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityContactService implements OwnerCheck {
  private final ContactService contactService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    try {
      final var contact = contactService.findById(id);

      return securityService.isOwner(contact.getUser().getId());
    } catch (Exception e) {
      return false;
    }
  }
}
