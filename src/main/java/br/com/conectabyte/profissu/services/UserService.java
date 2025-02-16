package br.com.conectabyte.profissu.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.repositories.UserRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }
}
