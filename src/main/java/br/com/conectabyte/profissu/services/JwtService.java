package br.com.conectabyte.profissu.services;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.entities.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
  @Value("${spring.application.name}")
  private String issuer;

  private final JwtEncoder jwtEncoder;

  public LoginResponseDto createJwtToken(User user) {
    final var now = Instant.now();
    final var expiresIn = 300L;
    final var scopes = user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.joining(" "));
    final var claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .subject(user.getId().toString())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(expiresIn))
        .claim("ROLE", scopes)
        .build();
    final var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

    return new LoginResponseDto(jwtValue, expiresIn);
  }

  public Optional<Map<String, Object>> getClaims() {
    final var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
      final var jwt = (Jwt) authentication.getPrincipal();

      return Optional.of(jwt.getClaims());
    }

    return Optional.empty();
  }
}
