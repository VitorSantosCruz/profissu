package br.com.conectabyte.profissu.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ResetPasswordResponseDto(@JsonIgnore Integer responseCode, String message) {

}
