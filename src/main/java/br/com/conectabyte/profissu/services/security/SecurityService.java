package br.com.conectabyte.profissu.services.security;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {
  private final JwtService jwtService;

  public boolean isOwner(Long userId) {
    log.debug("Performing ownership check for user ID: {}", userId);

    return this.jwtService.getClaims()
        .map(claims -> {
          final Long authenticatedUserId = Long.valueOf(claims.get("sub").toString());
          final boolean isOwner = authenticatedUserId.equals(userId);

          log.debug("Ownership check result for user ID {}: {}", userId, isOwner);
          return isOwner;
        })
        .orElseGet(() -> {
          log.debug("Ownership check failed: No claims found for user ID {}", userId);
          return false;
        });
  }

  public boolean isAdmin() {
    log.debug("Performing admin role check.");

    return this.jwtService.getClaims()
        .map(claims -> {
          final boolean isAdmin = String.valueOf(claims.get("ROLE")).contains(RoleEnum.ADMIN.name());

          log.debug("Admin role check result: {}", isAdmin);
          return isAdmin;
        })
        .orElseGet(() -> {
          log.debug("Admin role check failed: No claims found.");
          return false;
        });
  }
}
