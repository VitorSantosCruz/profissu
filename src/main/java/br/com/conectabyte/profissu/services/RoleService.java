package br.com.conectabyte.profissu.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {
  private final RoleRepository roleRepository;

  public Optional<Role> findByName(String name) {
    return roleRepository.findByName(name);
  }
}
