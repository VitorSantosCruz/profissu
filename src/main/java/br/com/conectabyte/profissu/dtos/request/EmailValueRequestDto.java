package br.com.conectabyte.profissu.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailValueRequestDto(
    @NotBlank(message = "email: Cannot be null or empty") @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "email: Must be a valid email address") String email) {
}
