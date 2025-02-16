package br.com.conectabyte.profissu.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record LoginRequestDto(@Email String email, @NotNull String password) {
}
