package br.com.conectabyte.profissu.dtos.response;

public record MessageResponseDto(Long id, String message, boolean read, UserResponseDto user) {
}
