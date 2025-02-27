package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.conectabyte.profissu.dtos.request.UserRequestDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.entities.User;

@Mapper(uses = { ContactMapper.class, AddressMapper.class, ProfileMapper.class })
public interface UserMapper {
  UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

  @Mapping(target = "conversationsAsARequester", ignore = true)
  @Mapping(target = "conversationsAsAServiceProvider", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "messages", ignore = true)
  @Mapping(target = "profile", ignore = true)
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "token", ignore = true)
  User userRequestDtoToUser(UserRequestDto userRequestDto);

  UserRequestDto userToUserRequestDto(User user);

  @Mapping(target = "conversationsAsARequester", ignore = true)
  @Mapping(target = "conversationsAsAServiceProvider", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "messages", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "token", ignore = true)
  User userResponseDtoToUser(UserResponseDto userResponseDto);

  UserResponseDto userToUserResponseDto(User user);
}
