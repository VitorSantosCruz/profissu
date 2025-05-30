package br.com.conectabyte.profissu.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationRequestDto(
    @NotNull(message = "requestedServiceId: Cannot be null or empty") @Min(value = 1, message = "requestedServiceId: cannot be less than 1") Long requestedServiceId,
    @NotBlank(message = "message: Cannot be null or empty") String message) {
}
