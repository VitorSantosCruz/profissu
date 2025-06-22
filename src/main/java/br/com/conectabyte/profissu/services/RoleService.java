package br.com.conectabyte.profissu.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {
  private final RoleRepository roleRepository;

  public Optional<Role> findByName(String name) {
    log.debug("Attempting to find role by name: {}", name);

    final var role = roleRepository.findByName(name);

    if (role.isPresent()) {
      log.debug("Found role with name: {}", name);
    } else {
      log.debug("Role with name: {} not found.", name);
    }

    return role;
  }
}
