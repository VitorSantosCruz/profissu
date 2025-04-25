package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.entities.Conversation;

@Mapper
public interface ConversationMapper {
  ConversationMapper INSTANCE = Mappers.getMapper(ConversationMapper.class);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "messages", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "requestedService", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "offerStatus", ignore = true)
  @Mapping(target = "requester", ignore = true)
  @Mapping(target = "serviceProvider", ignore = true)
  Conversation conversationRequestDtoToConversation(ConversationRequestDto conversationRequestDto);

  ConversationResponseDto conversationToConversationResponseDto(Conversation conversation);

  default Page<ConversationResponseDto> conversationPageToConversationResponseDtoPage(
      Page<Conversation> conversationPage) {
    final var conversationResponseDtoPageContent = conversationPage.getContent().stream()
        .map(this::conversationToConversationResponseDto)
        .toList();

    return new PageImpl<>(conversationResponseDtoPageContent, conversationPage.getPageable(),
        conversationPage.getTotalElements());
  }
}
