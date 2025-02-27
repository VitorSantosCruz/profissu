package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.conectabyte.profissu.dtos.request.ContactRequestDto;
import br.com.conectabyte.profissu.dtos.response.ContactResponseDto;
import br.com.conectabyte.profissu.entities.Contact;

@Mapper
public interface ContactMapper {
  ContactMapper INSTANCE = Mappers.getMapper(ContactMapper.class);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "verificationCompletedAt", ignore = true)
  @Mapping(target = "verificationRequestedAt", ignore = true)
  Contact contactRequestDtoToContact(ContactRequestDto contactRequestDto);

  ContactRequestDto contactToContactRequestDto(Contact contact);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "verificationCompletedAt", ignore = true)
  @Mapping(target = "verificationRequestedAt", ignore = true)
  Contact contactResponseDtoToContact(ContactResponseDto contactResponseDto);

  ContactResponseDto contactToContactResponseDto(Contact contact);
}
