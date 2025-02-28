package br.com.conectabyte.profissu.dtos.request;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordRequestDto(
    @NotBlank(message = "currentPassword: Cannot be null or empty") String currentPassword,
    @NotBlank(message = "newPassword: Cannot be null or empty") @Length(min = 8, message = "newPassword: Must have at least 8 characters") @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$", message = "newPassword: Must contain at least one uppercase letter, one digit, and one special character") String newPassword) {
}
