package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.services.ReviewService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityReviewService implements OwnerCheck {
  private final ReviewService reviewService;
  private final SecurityService securityService;

  @Override
  public boolean ownershipCheck(Long id) {
    try {
      final var address = reviewService.findById(id);

      return securityService.isOwner(address.getUser().getId());
    } catch (Exception e) {
      return false;
    }
  }
}
