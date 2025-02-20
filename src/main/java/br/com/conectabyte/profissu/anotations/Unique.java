package br.com.conectabyte.profissu.anotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.com.conectabyte.profissu.anotations.validators.UniqueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UniqueValidator.class)
public @interface Unique {
  String message() default "This value must be unique";

  public Class<?>[] groups() default {};

  public Class<? extends Payload>[] payload() default {};
}
