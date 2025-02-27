package br.com.conectabyte.profissu.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import br.com.conectabyte.profissu.utils.RoleUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {
  @Mock
  private JwtEncoder jwtEncoder;

  @InjectMocks
  private JwtService jwtService;

  @Test
  void shouldReturnTokenWhenSuccess() throws Exception {
    var issuerField = JwtService.class.getDeclaredField("issuer");
    issuerField.setAccessible(true);
    issuerField.set(jwtService, "profissu");

    final var user = UserUtils.create();
    final var map = Map.of("key", new Object());

    user.setRoles((Set.of(RoleUtils.create())));
    user.setId(1L);
    when(jwtEncoder.encode(any())).thenReturn(new Jwt("TOKEN", Instant.now(), Instant.now(), map, map));

    jwtService.createJwtToken(user);
  }
}
