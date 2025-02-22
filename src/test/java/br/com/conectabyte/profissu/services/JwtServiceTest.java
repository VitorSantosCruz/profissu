package br.com.conectabyte.profissu.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.utils.RoleUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@SpringBootTest
@ActiveProfiles("test")
public class JwtServiceTest {
  @MockBean
  private JwtEncoder jwtEncoder;

  @Autowired
  private JwtService jwtService;

  @Test
  void shouldReturnTokenWhenSuccess() {
    final var user = UserUtils.create();
    final var map = Map.of("key", new Object());

    user.setRoles((Set.of(RoleUtils.createRole())));
    user.setId(1L);
    when(jwtEncoder.encode(any())).thenReturn(new Jwt("TOKEN", Instant.now(), Instant.now(), map, map));

    jwtService.createJwtToken(user);
  }
}
