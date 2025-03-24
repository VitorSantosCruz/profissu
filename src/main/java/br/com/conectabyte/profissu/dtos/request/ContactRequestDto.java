package br.com.conectabyte.profissu.dtos.request;

import br.com.conectabyte.profissu.anotations.Unique;
import br.com.conectabyte.profissu.validators.groups.ValidatorGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContactRequestDto(
    @Unique(message = "contact: must be unique", groups = ValidatorGroup.class) @NotBlank(message = "value: Cannot be null or empty") @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "value: Must be a valid email address") String value,
    boolean standard) {
}