package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.dtos.response.MessageResponseDto;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.MessageMapper;
import br.com.conectabyte.profissu.repositories.MessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
  private final MessageRepository messageRepository;
  private final ConversationService conversationService;
  private final JwtService jwtService;
  private final UserService userService;
  private final SimpMessagingTemplate simpMessagingTemplate;

  private final MessageMapper messageMapper = MessageMapper.INSTANCE;

  public Message findById(Long id) {
    final var optionalMessage = messageRepository.findById(id);
    final var message = optionalMessage.orElseThrow(() -> new ResourceNotFoundException("Message not found."));

    return message;
  }

  @Transactional
  public Page<MessageResponseDto> listMessages(Long conversationId, Pageable pageable) {
    final var messages = messageRepository.listMessages(conversationId, pageable);
    return messageMapper.messagePageToMessageResponseDtoPage(messages);
  }

  @Transactional
  public MessageResponseDto sendMessage(Long conversationId, MessageRequestDto messageRequestDto) {
    final var conversation = conversationService.findById(conversationId);

    validateCanSendMessage(conversation);

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

    simpMessagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/messages", messageResponseDto);

    return messageResponseDto;
  }

  @Async
  public void markAsRead(Long id) {
    final var message = this.findById(id);

    message.setRead(true);
    messageRepository.save(message);
  }

  public List<Conversation> findConversationsWithUnreadMessages(LocalDateTime thresholdDate) {
    return messageRepository.findConversationsWithUnreadMessages(thresholdDate);
  }

  private void validateCanSendMessage(Conversation conversation) {
    final var requestedService = conversation.getRequestedService();

    if (!EnumSet.of(OfferStatusEnum.ACCEPTED, OfferStatusEnum.PENDING).contains(conversation.getOfferStatus())) {
      throw new ValidationException("This offer has already been canceled or rejected.");
    }

    if (!EnumSet.of(RequestedServiceStatusEnum.INPROGRESS, RequestedServiceStatusEnum.PENDING)
        .contains(requestedService.getStatus())) {
      throw new ValidationException(
          "The requested service associated with this offer has already been canceled or completed.");
    }
  }
}
