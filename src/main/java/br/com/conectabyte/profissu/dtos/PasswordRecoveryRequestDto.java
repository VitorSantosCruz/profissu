package br.com.conectabyte.profissu.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordRecoveryRequestDto(
    @NotBlank(message = "email: Cannot be null or empty") @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", message = "email: Must be a valid email address") String email) {
}
