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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
  private final MessageRepository messageRepository;
  private final ConversationService conversationService;
  private final JwtService jwtService;
  private final UserService userService;
  private final SimpMessagingTemplate simpMessagingTemplate;

  private final MessageMapper messageMapper = MessageMapper.INSTANCE;

  public Message findById(Long id) {
    log.debug("Attempting to find message by ID: {}", id);

    final var optionalMessage = messageRepository.findById(id);
    final var message = optionalMessage.orElseThrow(() -> {
      log.warn("Message with ID: {} not found.", id);
      return new ResourceNotFoundException("Message not found.");
    });

    log.debug("Found message with ID: {}", message.getId());
    return message;
  }

  @Transactional
  public Page<MessageResponseDto> listMessages(Long conversationId, Pageable pageable) {
    log.debug("Listing messages for conversation ID: {} with pageable: {}", conversationId, pageable);

    final var messages = messageRepository.listMessages(conversationId, pageable);

    log.debug("Found {} messages for conversation ID: {}", messages.getTotalElements(), conversationId);
    return messageMapper.messagePageToMessageResponseDtoPage(messages);
  }

  @Transactional
  public MessageResponseDto sendMessage(Long conversationId, MessageRequestDto messageRequestDto) {
    log.debug("Sending message for conversation ID: {} with message data: {}", conversationId,
        messageRequestDto.message());

    final var conversation = conversationService.findById(conversationId);

    log.debug("Found conversation ID: {}", conversation.getId());

    validateCanSendMessage(conversation);

    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("User ID from JWT: {}", userId);

    final var user = userService.findById(userId);
    final var message = Message.builder()
        .message(messageRequestDto.message())
        .conversation(conversation)
        .user(user)
        .build();
    final var messageResponseDto = messageMapper.messageToMessageResponseDto(messageRepository.save(message));

    log.debug("Sending message via WebSocket for conversation ID: {}", conversationId);
    simpMessagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/messages", messageResponseDto);
    log.info("Message sent successfully for conversation ID: {}", conversationId);
    return messageResponseDto;
  }

  @Async
  public void markAsRead(Long id) {
    log.debug("Attempting to mark message as read for ID: {}", id);

    final var message = this.findById(id);

    message.setRead(true);
    messageRepository.save(message);
    log.info("Message with ID: {} marked as read.", id);
  }

  public List<Conversation> findConversationsWithUnreadMessages(LocalDateTime thresholdDate) {
    log.debug("Finding conversations with unread messages older than: {}", thresholdDate);

    final var conversations = messageRepository.findConversationsWithUnreadMessages(thresholdDate);

    log.debug("Found {} conversations with unread messages.", conversations.size());
    return conversations;
  }

  private void validateCanSendMessage(Conversation conversation) {
    log.debug("Validating if message can be sent for conversation ID: {}", conversation.getId());

    final var requestedService = conversation.getRequestedService();

    if (!EnumSet.of(OfferStatusEnum.ACCEPTED, OfferStatusEnum.PENDING).contains(conversation.getOfferStatus())) {
      log.warn(
          "Validation failed for conversation ID {}: Offer status is not ACCEPTED or PENDING (current status: {}).",
          conversation.getId(), conversation.getOfferStatus());
      throw new ValidationException("This offer has already been canceled or rejected.");
    }

    if (!EnumSet.of(RequestedServiceStatusEnum.INPROGRESS, RequestedServiceStatusEnum.PENDING)
        .contains(requestedService.getStatus())) {
      log.warn(
          "Validation failed for conversation ID {}: Associated requested service status is not IN_PROGRESS or PENDING (current status: {}).",
          conversation.getId(), requestedService.getStatus());
      throw new ValidationException(
          "The requested service associated with this offer has already been canceled or completed.");
    }

    log.debug("Message can be sent for conversation ID: {}", conversation.getId());
  }
}
