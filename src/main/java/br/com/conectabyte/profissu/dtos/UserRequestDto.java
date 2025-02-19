package br.com.conectabyte.profissu.dtos;

import java.util.List;

import org.hibernate.validator.constraints.Length;

import br.com.conectabyte.profissu.enums.GenderEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserRequestDto(
    @NotBlank @Length(min = 5) String name,
    @NotBlank @Length(min = 8) @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,}$") String password,
    @NotNull GenderEnum gender,
    @NotNull List<ContactRequestDto> contacts,
    @NotNull List<AddressRequestDto> addresses) {
}
