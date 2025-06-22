package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityContactService implements OwnerCheck {
  private final ContactService contactService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    log.debug("Performing ownership check for contact ID: {}", id);

    try {
      final var contact = contactService.findById(id);
      final var isOwner = securityService.isOwner(contact.getUser().getId());

      log.debug("Ownership check result for contact ID {}: {}", id, isOwner);
      return isOwner;
    } catch (Exception e) {
      log.debug("Ownership check for contact ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }
}
