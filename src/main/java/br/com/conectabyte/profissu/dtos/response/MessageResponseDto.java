package br.com.conectabyte.profissu.dtos.response;

public record MessageResponseDto(Long id, boolean read, UserResponseDto user, ConversationResponseDto conversation) {
}
