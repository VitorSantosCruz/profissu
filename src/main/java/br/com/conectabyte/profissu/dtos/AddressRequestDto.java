package br.com.conectabyte.profissu.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressRequestDto(
    @NotBlank String street,
    @NotBlank String number,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank @Pattern(regexp = "^[0-9]{5}-[0-9]{3}$") String zipCode) {
}
