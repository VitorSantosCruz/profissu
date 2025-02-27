package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.conectabyte.profissu.dtos.request.ProfileRequestDto;
import br.com.conectabyte.profissu.dtos.response.ProfileResponseDto;
import br.com.conectabyte.profissu.entities.Profile;

@Mapper
public interface ProfileMapper {
  ProfileMapper INSTANCE = Mappers.getMapper(ProfileMapper.class);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  Profile profileRequestDtoToProfile(ProfileRequestDto profileRequestDto);

  ProfileRequestDto profileToProfileRequestDto(Profile profile);

  @Mapping(target = "user", ignore = true)
  Profile profileResponseDtoToProfile(ProfileResponseDto profileResponseDto);

  ProfileResponseDto profileToProfileResponseDto(Profile profile);
}
