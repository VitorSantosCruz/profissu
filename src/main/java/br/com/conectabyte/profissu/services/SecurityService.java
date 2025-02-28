package br.com.conectabyte.profissu.services;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.enums.RoleEnum;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityService {
  private final JwtService jwtService;

  public boolean isOwner(Long userId) {
    return this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()).equals(userId))
        .orElse(false);
  }

  public boolean isAdmin() {
    return this.jwtService.getClaims()
        .map(claims -> String.valueOf(claims.get("ROLE")).contains(RoleEnum.ADMIN.name()))
        .orElse(false);
  }

}
