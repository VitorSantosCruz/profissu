package br.com.conectabyte.profissu.dtos;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequestDto(
    @Email(message = "email: Must be a valid email address") @NotBlank(message = "email: Cannot be null or empty") String email,
    @NotBlank(message = "password: Cannot be null or empty") @Length(min = 8, message = "password: Must have at least 8 characters") @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$", message = "password: Must contain at least one uppercase letter, one digit, and one special character") String password,
    @NotBlank(message = "resetCode: Cannot be null or empty") String resetCode) {
}
