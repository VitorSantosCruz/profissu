package br.com.conectabyte.profissu.dtos.response;

public record LoginResponseDto(String accessToken, Long expiresIn) {
}
