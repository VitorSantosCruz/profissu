package br.com.conectabyte.profissu.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record MessageValueResponseDto(@JsonIgnore Integer responseCode, String message) {
}
