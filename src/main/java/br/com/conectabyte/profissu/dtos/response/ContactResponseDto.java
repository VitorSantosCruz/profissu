package br.com.conectabyte.profissu.dtos.response;

import br.com.conectabyte.profissu.enums.ContactTypeEnum;

public record ContactResponseDto(Long id, ContactTypeEnum type, String value, boolean standard) {
}
