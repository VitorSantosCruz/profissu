package br.com.conectabyte.profissu.services;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
  private final JwtEncoder jwtEncoder;
  private final ProfissuProperties profissuProperties;

  public LoginResponseDto createJwtToken(User user) {
    log.debug("Creating JWT token for user ID: {}", user.getId());
    
    final var now = Instant.now();
    final var expiresIn = profissuProperties.getProfissu().getJwt().getExpiresIn();
    final var scopes = user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.joining(" "));
    final var claims = JwtClaimsSet.builder()
        .issuer(profissuProperties.getSpring().getApplication().getName())
        .subject(user.getId().toString())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(expiresIn))
        .claim("ROLE", scopes)
        .build();
    final var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

    log.info("JWT token successfully created for user ID: {}. Expires in {} seconds.", user.getId(), expiresIn);
    return new LoginResponseDto(jwtValue, expiresIn);
  }

  public Optional<Map<String, Object>> getClaims() {
    log.debug("Attempting to retrieve JWT claims from SecurityContextHolder.");

    final var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
      final var jwt = (Jwt) authentication.getPrincipal();

      log.debug("JWT claims successfully retrieved for subject: {}", jwt.getSubject());
      return Optional.of(jwt.getClaims());
    }

    log.debug("No JWT found in SecurityContextHolder or principal is not a JWT.");
    return Optional.empty();
  }
}
