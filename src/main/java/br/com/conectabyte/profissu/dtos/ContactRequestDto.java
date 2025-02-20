package br.com.conectabyte.profissu.dtos;

import br.com.conectabyte.profissu.anotations.Unique;
import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ContactRequestDto(
        @NotNull(message = "type: Cannot be null") ContactTypeEnum type,
        @Unique(message = "contact: must be unique") @NotBlank(message = "value: Cannot be null or empty") @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "value: Must be a valid email address") String value,
        boolean standard) {
}