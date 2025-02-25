package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.repositories.RoleRepository;
import br.com.conectabyte.profissu.utils.RoleUtils;

@SpringBootTest
@ActiveProfiles("test")
public class RoleServiceTest {
  @Mock
  private RoleRepository roleRepository;

  @InjectMocks
  private RoleService roleService;

  @Test
  void shouldFindRoleByNameSuccessfully() {
    final var roleName = RoleEnum.USER.name();
    when(roleRepository.findByName(any())).thenReturn(Optional.of(RoleUtils.create(roleName)));

    final var role = roleService.findByName(roleName);

    assertTrue(role.isPresent());
    assertTrue(role.get().getName().equals("USER"));
  }
}
