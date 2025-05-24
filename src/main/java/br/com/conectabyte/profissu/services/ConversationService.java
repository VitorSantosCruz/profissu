package br.com.conectabyte.profissu.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.dtos.response.MessageResponseDto;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ConversationMapper;
import br.com.conectabyte.profissu.mappers.MessageMapper;
import br.com.conectabyte.profissu.repositories.ConversationRepository;
import br.com.conectabyte.profissu.repositories.MessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationService {
  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final RequestedServiceService requestedServiceService;
  private final JwtService jwtService;
  private final UserService userService;
  private final SimpMessagingTemplate simpMessagingTemplate;

  private final ConversationMapper conversationMapper = ConversationMapper.INSTANCE;
  private final MessageMapper messageMapper = MessageMapper.INSTANCE;

  public Conversation findById(Long id) {
    final var optionalConversation = conversationRepository.findById(id);
    final var conversation = optionalConversation
        .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

    return conversation;
  }

  @Transactional
  public Page<ConversationResponseDto> findByUserId(Long userId, Pageable pageable) {
    final var conversations = conversationRepository.findByUserId(userId, pageable);
    return conversationMapper.conversationPageToConversationResponseDtoPage(conversations);
  }

  @Transactional
  public ConversationResponseDto start(ConversationRequestDto conversationRequestDto) {
    final var requestedService = requestedServiceService.findById(conversationRequestDto.requestedServiceId());
    final var serviceProviderId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();
    final var serviceProvider = userService.findById(serviceProviderId);
    final var conversation = conversationMapper.conversationRequestDtoToConversation(conversationRequestDto);
    final var message = Message.builder()
        .message(conversationRequestDto.message())
        .conversation(conversation)
        .user(serviceProvider)
        .build();
    final var alreadySubmittedAnOffer = requestedService.getConversations().stream()
        .filter(c -> c.getOfferStatus() == OfferStatusEnum.PENDING)
        .filter(c -> c.getServiceProvider().getId().equals(serviceProviderId))
        .findFirst()
        .isPresent();

    this.validate(requestedService, serviceProvider, alreadySubmittedAnOffer);
    conversation.setServiceProvider(serviceProvider);
    conversation.setRequester(requestedService.getUser());
    conversation.setRequestedService(requestedService);
    conversation.setMessages(List.of(message));

    return conversationMapper.conversationToConversationResponseDto(conversationRepository.save(conversation));
  }

  @Transactional
  public MessageResponseDto sendMessage(Long id, MessageRequestDto messageRequestDto) {
    final var conversation = this.findById(id);
    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();
    final var user = userService.findById(userId);
    final var message = Message.builder()
        .message(messageRequestDto.message())
        .conversation(conversation)
        .user(user)
        .build();
    final var messageResponseDto = messageMapper.messageToMessageResponseDto(messageRepository.save(message));

    simpMessagingTemplate.convertAndSend("/topic/conversations/" + id + "/messages", messageResponseDto);

    return messageResponseDto;
  }

  @Transactional
  public ConversationResponseDto cancel(Long id) {
    final var conversation = findById(id);

    if (conversation.getOfferStatus() != OfferStatusEnum.PENDING) {
      throw new ValidationException("Conversation cannot be canceled.");
    }

    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);

    return conversationMapper.conversationToConversationResponseDto(conversationRepository.save(conversation));
  }

  @Transactional
  public ConversationResponseDto acceptOrRejectOffer(Long id, OfferStatusEnum offerStatus) {
    final var conversation = findById(id);

    if (offerStatus != OfferStatusEnum.ACCEPTED && offerStatus != OfferStatusEnum.REJECTED) {
      throw new ValidationException("Offer status is invalid.");
    }

    if (conversation.getOfferStatus() != OfferStatusEnum.PENDING) {
      throw new ValidationException("Conversation cannot be accepted/rejected.");
    }

    conversation.setOfferStatus(offerStatus);

    return conversationMapper.conversationToConversationResponseDto(conversationRepository.save(conversation));
  }

  private void validate(RequestedService requestedService, User serviceProvider, boolean alreadySubmittedAnOffer) {
    if (requestedService.getStatus() != RequestedServiceStatusEnum.PENDING) {
      throw new ValidationException("Cannot make an offer for this requested service.");
    }

    if (serviceProvider.getId().equals(requestedService.getUser().getId())) {
      throw new ValidationException("You cannot submit an offer for your own requested service.");
    }

    if (alreadySubmittedAnOffer) {
      throw new ValidationException("You have already submitted an offer for this requested service.");
    }
  }
}
