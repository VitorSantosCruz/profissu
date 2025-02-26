package br.com.conectabyte.profissu.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProfileResponseDto(Long id, String bio) {
}
