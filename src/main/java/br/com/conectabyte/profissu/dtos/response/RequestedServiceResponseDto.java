package br.com.conectabyte.profissu.dtos.response;

import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;

public record RequestedServiceResponseDto(Long id, String title, String description, RequestedServiceStatusEnum status,
    AddressResponseDto address, UserResponseDto user) {
}
