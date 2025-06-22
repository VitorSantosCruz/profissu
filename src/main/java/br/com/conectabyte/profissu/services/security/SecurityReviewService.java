package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityReviewService implements OwnerCheck {
  private final ReviewService reviewService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    log.debug("Performing ownership check for review ID: {}", id);

    try {
      final var address = reviewService.findById(id);
      final var isOwner = securityService.isOwner(address.getUser().getId());

      log.debug("Ownership check result for review ID {}: {}", id, isOwner);
      return isOwner;
    } catch (Exception e) {
      log.debug("Ownership check for review ID {} failed due to exception: {}", id, e.getMessage());
      return false;
    }
  }
}
