package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.utils.PropertiesLoader;
import br.com.conectabyte.profissu.utils.RoleUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {
  @Mock
  private JwtEncoder jwtEncoder;

  @Mock
  private ProfissuProperties profissuProperties;

  @InjectMocks
  private JwtService jwtService;

  void setUp() throws Exception {
    final var loadedProfissuProperties = new PropertiesLoader().loadProperties();

    when(profissuProperties.getProfissu()).thenReturn(loadedProfissuProperties.getProfissu());
    when(profissuProperties.getSpring()).thenReturn(loadedProfissuProperties.getSpring());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnTokenWhenSuccess() throws Exception {
    setUp();
    final var user = UserUtils.create();
    final var now = Instant.now();
    final var map = Map.of("key", new Object());

    user.setRoles(Set.of(RoleUtils.create()));
    user.setId(1L);

    when(jwtEncoder.encode(any())).thenReturn(new Jwt("TOKEN", now, now.plusSeconds(1), map, map));

    final var response = jwtService.createJwtToken(user);

    assertEquals("TOKEN", response.accessToken());
    assertEquals(300L, response.expiresIn());

    verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
  }

  @Test
  void shouldContainCorrectClaims() throws Exception {
    setUp();
    final var user = UserUtils.create();
    final var now = Instant.now();
    final var map = Map.of("key", new Object());

    user.setRoles(Set.of(RoleUtils.create()));
    user.setId(1L);

    when(jwtEncoder.encode(any())).thenReturn(new Jwt("FAKE_TOKEN", now, now.plusSeconds(1), map, map));

    final var response = jwtService.createJwtToken(user);

    assertEquals("FAKE_TOKEN", response.accessToken());
    assertEquals(300L, response.expiresIn());
  }

  @Test
  void shouldReturnClaimsWhenJwtIsPresent() {
    final Map<String, Object> claims = Map.of();
    final var jwt = mock(Jwt.class);
    final var authentication = mock(Authentication.class);
    final var securityContext = mock(SecurityContext.class);

    when(jwt.getClaims()).thenReturn(claims);
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);

    final var result = jwtService.getClaims();

    assertTrue(result.isPresent());
    assertEquals(claims, result.get());
  }

  @Test
  void shouldReturnEmptyWhenAuthenticationIsNull() {
    final var securityContext = mock(SecurityContext.class);

    when(securityContext.getAuthentication()).thenReturn(null);

    SecurityContextHolder.setContext(securityContext);

    final var result = jwtService.getClaims();

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptyWhenPrincipalIsNotJwt() {
    final var authentication = mock(Authentication.class);
    final var securityContext = mock(SecurityContext.class);

    when(authentication.getPrincipal()).thenReturn("Not a Jwt");
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);

    final var result = jwtService.getClaims();

    assertTrue(result.isEmpty());
  }
}
