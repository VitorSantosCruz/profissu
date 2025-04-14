package br.com.conectabyte.profissu.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationRequestDto(
    @NotNull(message = "requestedServiceId: Cannot be null or empty") Long requestedServiceId,
    @NotBlank(message = "message: Cannot be null or empty") String message) {
}
