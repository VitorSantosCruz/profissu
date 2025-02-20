package br.com.conectabyte.profissu.validators;

import org.springframework.beans.factory.annotation.Autowired;

import br.com.conectabyte.profissu.anotations.Unique;
import br.com.conectabyte.profissu.repositories.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueValidator implements ConstraintValidator<Unique, String> {
  @Autowired
  private UserRepository userRepository;

  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    return userRepository.findByEmail(email).isEmpty();
  }
}