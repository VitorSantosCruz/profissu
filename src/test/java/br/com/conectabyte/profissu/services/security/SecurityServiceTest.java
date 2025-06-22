package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.services.JwtService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityService Tests")
public class SecurityServiceTest {
  @Mock
  private JwtService jwtService;

  @InjectMocks
  private SecurityService securityService;

  private static final Long AUTHENTICATED_USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final String ADMIN_ROLE = RoleEnum.ADMIN.name();
  private static final String USER_ROLE = RoleEnum.USER.name();
  private static final String MULTIPLE_ROLES = RoleEnum.ADMIN.name() + " " + RoleEnum.USER.name();

  @Test
  @DisplayName("Should return true when authenticated user ID matches provided user ID")
  void shouldReturnTrueWhenAuthenticatedIdMatchesProvidedId() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", String.valueOf(AUTHENTICATED_USER_ID))));

    boolean isOwner = securityService.isOwner(AUTHENTICATED_USER_ID);

    assertTrue(isOwner);
    verify(jwtService, times(1)).getClaims();
  }

  @Test
  @DisplayName("Should return false when authenticated user ID does not match provided user ID")
  void shouldReturnFalseWhenAuthenticatedIdDoesNotMatchProvidedId() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", String.valueOf(AUTHENTICATED_USER_ID))));

    boolean isOwner = securityService.isOwner(OTHER_USER_ID);

    assertFalse(isOwner);
    verify(jwtService, times(1)).getClaims();
  }

  @Test
  @DisplayName("Should return false when no claims are found for ownership check")
  void shouldReturnFalseWhenNoClaimsForOwnershipCheck() {
    when(jwtService.getClaims()).thenReturn(Optional.empty());

    boolean isOwner = securityService.isOwner(AUTHENTICATED_USER_ID);

    assertFalse(isOwner);
    verify(jwtService, times(1)).getClaims();
  }

  @Test
  @DisplayName("Should return true when user has ADMIN role")
  void shouldReturnTrueWhenUserHasAdminRole() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("ROLE", ADMIN_ROLE)));

    boolean isAdmin = securityService.isAdmin();

    assertTrue(isAdmin);
    verify(jwtService, times(1)).getClaims();
  }

  @Test
  @DisplayName("Should return true when user has ADMIN role among multiple roles")
  void shouldReturnTrueWhenUserHasAdminRoleAmongMultipleRoles() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("ROLE", MULTIPLE_ROLES)));

    boolean isAdmin = securityService.isAdmin();

    assertTrue(isAdmin);
    verify(jwtService, times(1)).getClaims();
  }

  @Test
  @DisplayName("Should return false when user does not have ADMIN role")
  void shouldReturnFalseWhenUserDoesNotHaveAdminRole() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("ROLE", USER_ROLE)));

    boolean isAdmin = securityService.isAdmin();

    assertFalse(isAdmin);
    verify(jwtService, times(1)).getClaims();
  }

  @Test
  @DisplayName("Should return false when no claims are found for admin check")
  void shouldReturnFalseWhenNoClaimsForAdminCheck() {
    when(jwtService.getClaims()).thenReturn(Optional.empty());

    boolean isAdmin = securityService.isAdmin();

    assertFalse(isAdmin);
    verify(jwtService, times(1)).getClaims();
  }

  @Test
  @DisplayName("Should return false when ROLE claim is missing or not a String for admin check")
  void shouldReturnFalseWhenRoleClaimIsMissing() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("OTHER_CLAIM", "VALUE")));

    boolean isAdmin = securityService.isAdmin();

    assertFalse(isAdmin);
    verify(jwtService, times(1)).getClaims();
  }
}
