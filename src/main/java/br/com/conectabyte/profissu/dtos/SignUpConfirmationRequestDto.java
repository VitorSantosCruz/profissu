package br.com.conectabyte.profissu.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpConfirmationRequestDto(
    @Email(message = "email: Must be a valid email address") @NotBlank(message = "email: Cannot be null or empty") String email,
    @NotBlank(message = "resetCode: Cannot be null or empty") String resetCode) {
}
