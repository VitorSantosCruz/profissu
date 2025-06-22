package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.repositories.RoleRepository;
import br.com.conectabyte.profissu.utils.RoleUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Tests")
public class RoleServiceTest {
  @Mock
  private RoleRepository roleRepository;

  @InjectMocks
  private RoleService roleService;

  private static final String USER_ROLE_NAME = RoleEnum.USER.name();
  private static final String NON_EXISTENT_ROLE_NAME = "NON_EXISTENT_ROLE";

  @Test
  @DisplayName("Should find role by name successfully when role exists")
  void shouldFindRoleByNameSuccessfully() {
    Role mockRole = RoleUtils.create(USER_ROLE_NAME);
    when(roleRepository.findByName(eq(USER_ROLE_NAME))).thenReturn(Optional.of(mockRole));

    Optional<Role> foundRole = roleService.findByName(USER_ROLE_NAME);

    assertTrue(foundRole.isPresent());
    assertEquals(USER_ROLE_NAME, foundRole.get().getName());
    verify(roleRepository, times(1)).findByName(eq(USER_ROLE_NAME));
  }

  @Test
  @DisplayName("Should return empty Optional when role not found by name")
  void shouldReturnEmptyOptionalWhenRoleNotFoundByName() {
    when(roleRepository.findByName(eq(NON_EXISTENT_ROLE_NAME))).thenReturn(Optional.empty());

    Optional<Role> foundRole = roleService.findByName(NON_EXISTENT_ROLE_NAME);

    assertFalse(foundRole.isPresent());
    verify(roleRepository, times(1)).findByName(eq(NON_EXISTENT_ROLE_NAME));
  }
}
