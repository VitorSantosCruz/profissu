package br.com.conectabyte.profissu.dtos.request;

import org.hibernate.validator.constraints.Length;

import br.com.conectabyte.profissu.enums.GenderEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileRequestDto(
    @NotBlank(message = "name: Cannot be null or empty") @Length(min = 5, message = "name: Must have at least 5 characters") String name,
    String bio,
    @NotNull(message = "gender: Cannot be null") GenderEnum gender) {
}
