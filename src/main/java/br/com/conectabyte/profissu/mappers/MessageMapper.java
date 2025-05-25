package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import br.com.conectabyte.profissu.dtos.response.MessageResponseDto;
import br.com.conectabyte.profissu.entities.Message;

@Mapper
public interface MessageMapper {
  MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

  MessageResponseDto messageToMessageResponseDto(Message message);

  default Page<MessageResponseDto> messagePageToMessageResponseDtoPage(
      Page<Message> messagePage) {
    final var messageResponseDtoPageContent = messagePage.getContent().stream()
        .map(this::messageToMessageResponseDto)
        .toList();

    return new PageImpl<>(messageResponseDtoPageContent, messagePage.getPageable(),
        messagePage.getTotalElements());
  }
}
