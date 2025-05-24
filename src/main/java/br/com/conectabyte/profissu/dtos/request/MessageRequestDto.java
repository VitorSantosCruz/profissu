package br.com.conectabyte.profissu.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record MessageRequestDto(
  @NotBlank(message = "message: Cannot be null or empty") String message) {
}
