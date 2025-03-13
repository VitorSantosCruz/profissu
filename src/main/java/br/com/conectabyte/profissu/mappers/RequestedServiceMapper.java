package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.entities.RequestedService;

@Mapper(uses = { UserMapper.class, AddressMapper.class })
public interface RequestedServiceMapper {
  RequestedServiceMapper INSTANCE = Mappers.getMapper(RequestedServiceMapper.class);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "status", ignore = true)
  RequestedService requestedServiceRequestDtoToRequestedService(RequestedServiceRequestDto requestedServiceRequestDto);

  @Mapping(source = "user", target = "requesterId", qualifiedByName = "matUserToId")
  RequestedServiceResponseDto requestedServiceToRequestedServiceResponseDto(
      RequestedService requestedServiceRequestDto);
}
