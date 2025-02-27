package br.com.conectabyte.profissu.dtos.response;

public record AddressResponseDto(Long id, String street, String number, String city, String state, String zipCode) {
}
