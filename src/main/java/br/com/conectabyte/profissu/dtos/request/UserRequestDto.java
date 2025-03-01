package br.com.conectabyte.profissu.dtos.request;

import java.util.List;

import org.hibernate.validator.constraints.Length;

import br.com.conectabyte.profissu.enums.GenderEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserRequestDto(
    @NotBlank(message = "name: Cannot be null or empty") @Length(min = 5, message = "name: Must have at least 5 characters") String name,
    @NotBlank(message = "password: Cannot be null or empty") @Length(min = 8, message = "password: Must have at least 8 characters") @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$", message = "password: Must contain at least one uppercase letter, one digit, and one special character") String password,
    @NotNull(message = "gender: Cannot be null") GenderEnum gender,
    @Valid @NotNull(message = "contacts: Cannot be null") List<ContactRequestDto> contacts,
    @Valid @NotNull(message = "addresses: Cannot be null") List<AddressRequestDto> addresses) {
}