package br.com.conectabyte.profissu.dtos.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ExceptionDto(String message, List<String> errors) {
}
