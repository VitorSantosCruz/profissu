package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

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

  default Page<RequestedServiceResponseDto> RequestedServicePageToRequestedServiceResponseDtoPage(
      Page<RequestedService> requestedServicePage) {
    final var requestedServiceResponseDtoPageContent = requestedServicePage.getContent().stream()
        .map(this::requestedServiceToRequestedServiceResponseDto)
        .toList();

    return new PageImpl<>(requestedServiceResponseDtoPageContent, requestedServicePage.getPageable(),
        requestedServicePage.getTotalElements());
  }
}
