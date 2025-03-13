package br.com.conectabyte.profissu.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestedServiceRequestDto(
    @NotBlank(message = "title: Cannot be null or empty") String title,
    @NotBlank(message = "description: Cannot be null or empty") String description,
    @Valid @NotNull(message = "address: Cannot be null") AddressRequestDto address) {
}
