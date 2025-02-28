package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
  @Mock
  private JwtService jwtService;

  @InjectMocks
  private SecurityService securityService;

  @Test
  void shouldReturnTrueWhenInformetedIDIsSameThenToken() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));

    final var isOwner = securityService.isOwner(1L);

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenTheInformetedIDNNotIsSameThenToken() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "0")));

    final var isOwner = securityService.isOwner(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserHaveAdminRole() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("ROLE", "ADMIN")));

    final var isAdmin = securityService.isAdmin();

    assertTrue(isAdmin);
  }

  @Test
  void shouldReturnFalseWhenUserNotHaveAdminRole() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("ROLE", "USER")));

    final var isAdmin = securityService.isAdmin();

    assertFalse(isAdmin);
  }
}
