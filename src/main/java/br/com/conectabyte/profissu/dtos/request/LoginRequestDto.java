package br.com.conectabyte.profissu.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @Email(message = "email: Must be a valid email address") @NotBlank(message = "email: Cannot be null or empty") String email,
    @NotBlank(message = "password: Cannot be null or empty") String password) {
}
