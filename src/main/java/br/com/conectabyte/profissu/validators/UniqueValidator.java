package br.com.conectabyte.profissu.validators;

import org.springframework.beans.factory.annotation.Autowired;

import br.com.conectabyte.profissu.anotations.Unique;
import br.com.conectabyte.profissu.services.UserService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueValidator implements ConstraintValidator<Unique, String> {
  @Autowired
  private UserService userService;

  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    try {
      userService.findByEmail(email);

      return false;
    } catch (Exception e) {
      return true;
    }
  }
}