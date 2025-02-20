package br.com.conectabyte.profissu.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressRequestDto(
    @NotBlank(message = "street: Cannot be null or empty") String street,
    @NotBlank(message = "number: Cannot be null or empty") String number,
    @NotBlank(message = "city: Cannot be null or empty") String city,
    @NotBlank(message = "state: Cannot be null or empty") String state,
    @NotBlank(message = "zipCode: Cannot be null or empty") @Pattern(regexp = "^[0-9]{5}-[0-9]{3}$", message = "zipCode: Must match the pattern '00000-000'") String zipCode) {
}
