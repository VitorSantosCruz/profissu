package br.com.conectabyte.profissu.dtos.response;

public record ReviewResponseDto(Long id, String title, String review, int stars,
    UserResponseDto user, RequestedServiceResponseDto requestedService) {
}
