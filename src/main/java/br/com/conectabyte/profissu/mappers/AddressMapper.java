package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.conectabyte.profissu.dtos.request.AddressRequestDto;
import br.com.conectabyte.profissu.dtos.response.AddressResponseDto;
import br.com.conectabyte.profissu.entities.Address;

@Mapper
public interface AddressMapper {
  AddressMapper INSTANCE = Mappers.getMapper(AddressMapper.class);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "requestedService", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "user", ignore = true)
  Address addressRequestDtoToAddress(AddressRequestDto addressRequestDto);

  AddressRequestDto addressToAddressRequestDto(Address address);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "requestedService", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "user", ignore = true)
  Address addressResponseDtoToAddress(AddressResponseDto addressResponseDto);

  AddressResponseDto addressToAddressResponseDto(Address address);
}
