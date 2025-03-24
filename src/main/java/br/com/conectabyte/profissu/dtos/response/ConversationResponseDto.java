package br.com.conectabyte.profissu.dtos.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import br.com.conectabyte.profissu.enums.OfferStatusEnum;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public record ConversationResponseDto(Long id, OfferStatusEnum offerStatus, UserResponseDto requester,
    UserResponseDto serviceProvider, List<MessageResponseDto> messages) {
}
