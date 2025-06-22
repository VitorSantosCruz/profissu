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
import org.junit.jupiter.api.DisplayName;
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

import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.utils.PropertiesLoader;
import br.com.conectabyte.profissu.utils.RoleUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
public class JwtServiceTest {

  @Mock
  private JwtEncoder jwtEncoder;

  @Mock
  private ProfissuProperties profissuProperties;

  @InjectMocks
  private JwtService jwtService;

  private static final long JWT_EXPIRES_IN_SECONDS = 300L;
  private static final String FAKE_TOKEN_VALUE = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

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
  @DisplayName("Should create JWT token successfully with correct token value and expiry")
  void shouldCreateJwtTokenSuccessfully() throws Exception {
    setUp();
    final var user = UserUtils.create();
    final var now = Instant.now();
    final var map = Map.of("key", new Object());

    user.setRoles(Set.of(RoleUtils.create()));
    user.setId(1L);

    when(jwtEncoder.encode(any())).thenReturn(new Jwt(FAKE_TOKEN_VALUE, now, now.plusSeconds(1), map, map));

    final var response = jwtService.createJwtToken(user);

    assertEquals(FAKE_TOKEN_VALUE, response.accessToken());
    assertEquals(JWT_EXPIRES_IN_SECONDS, response.expiresIn());
  }

  @Test
  @DisplayName("Should return claims when JWT is present in SecurityContextHolder")
  void shouldReturnClaimsWhenJwtIsPresent() {
    final Map<String, Object> expectedClaims = Map.of();
    final var jwt = mock(Jwt.class);
    final var authentication = mock(Authentication.class);
    final var securityContext = mock(SecurityContext.class);

    when(jwt.getClaims()).thenReturn(expectedClaims);
    when(jwt.getSubject()).thenReturn("123");
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);

    final var result = jwtService.getClaims();

    assertTrue(result.isPresent());
    assertEquals(expectedClaims, result.get());

    verify(securityContext).getAuthentication();
    verify(jwt).getClaims();
  }

  @Test
  @DisplayName("Should return empty Optional when Authentication is null in SecurityContextHolder")
  void shouldReturnEmptyWhenAuthenticationIsNull() {
    final var securityContext = mock(SecurityContext.class);

    when(securityContext.getAuthentication()).thenReturn(null);

    SecurityContextHolder.setContext(securityContext);

    final var result = jwtService.getClaims();

    assertTrue(result.isEmpty());

    verify(securityContext).getAuthentication();
  }

  @Test
  @DisplayName("Should return empty Optional when principal is not a Jwt instance")
  void shouldReturnEmptyWhenPrincipalIsNotJwt() {
    final var authentication = mock(Authentication.class);
    final var securityContext = mock(SecurityContext.class);

    when(authentication.getPrincipal()).thenReturn("Some other principal type");
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);

    final var result = jwtService.getClaims();

    assertTrue(result.isEmpty());
    verify(securityContext).getAuthentication();
    verify(authentication).getPrincipal();
  }
}
