package br.com.conectabyte.profissu.dtos;

import java.util.List;

import br.com.conectabyte.profissu.enums.GenderEnum;

public record UserResponseDto(Long id, String name, GenderEnum gender, ProfileResponseDto profile,
    List<ContactResponseDto> contacts, List<AddressResponseDto> addresses) {
}
