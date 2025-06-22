package br.com.conectabyte.profissu.validators;

import org.springframework.beans.factory.annotation.Autowired;

import br.com.conectabyte.profissu.anotations.Unique;
import br.com.conectabyte.profissu.services.UserService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UniqueValidator implements ConstraintValidator<Unique, String> {
  @Autowired
  private UserService userService;

  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    log.debug("Validating uniqueness of email: {}", email);

    try {
      userService.findByEmail(email);
      log.debug("Email {} already exists. Validation failed.", email);
      return false;
    } catch (Exception e) {
      log.debug("Email {} is unique. Validation successful. Exception message: {}", email, e.getMessage());
      return true;
    }
  }
}