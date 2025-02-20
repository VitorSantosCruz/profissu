package br.com.conectabyte.profissu.dtos;

import br.com.conectabyte.profissu.enums.ContactTypeEnum;

public record ContactResponseDto(Long id, ContactTypeEnum type, String value, boolean standard) {
}
