package br.com.conectabyte.profissu.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ConversationMapper;
import br.com.conectabyte.profissu.repositories.ConversationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {
  private final ConversationRepository conversationRepository;
  private final RequestedServiceService requestedServiceService;
  private final JwtService jwtService;
  private final UserService userService;

  private final ConversationMapper conversationMapper = ConversationMapper.INSTANCE;

  public Conversation findById(Long id) {
    log.debug("Attempting to find conversation by ID: {}", id);

    final var optionalConversation = conversationRepository.findById(id);
    final var conversation = optionalConversation
        .orElseThrow(() -> {
          log.warn("Conversation with ID: {} not found.", id);
          return new ResourceNotFoundException("Conversation not found.");
        });

    log.debug("Found conversation with ID: {}", conversation.getId());
    return conversation;
  }

  @Transactional
  public Page<ConversationResponseDto> findCurrentUserConversations(Pageable pageable) {
    log.debug("Finding current user conversations with pageable: {}", pageable);

    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("Retrieved user ID from JWT: {}", userId);

    final var conversations = conversationRepository.findByUserId(userId, pageable);

    log.debug("Found {} conversations for user ID: {}", conversations.getTotalElements(), userId);
    return conversationMapper.conversationPageToConversationResponseDtoPage(conversations);
  }

  @Transactional
  public ConversationResponseDto start(ConversationRequestDto conversationRequestDto) {
    log.debug("Starting new conversation with data: {}", conversationRequestDto);

    final var requestedService = requestedServiceService.findById(conversationRequestDto.requestedServiceId());
    final var serviceProviderId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("Service provider ID from JWT: {}", serviceProviderId);

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

    log.debug("Service provider already submitted an offer: {}", alreadySubmittedAnOffer);

    this.validateNewOffers(requestedService, serviceProvider, alreadySubmittedAnOffer);
    conversation.setServiceProvider(serviceProvider);
    conversation.setRequester(requestedService.getUser());
    conversation.setRequestedService(requestedService);
    conversation.setMessages(List.of(message));

    final var savedConversation = conversationRepository.save(conversation);

    log.info("Conversation started successfully with ID: {} for requested service ID: {}", savedConversation.getId(),
        requestedService.getId());
    return conversationMapper.conversationToConversationResponseDto(savedConversation);
  }

  @Transactional
  public ConversationResponseDto changeOfferStatus(Long id, OfferStatusEnum offerStatus) {
    log.debug("Changing offer status for conversation ID: {} to status: {}", id, offerStatus);

    final var conversation = findById(id);

    validateChangeOfferStatus(conversation, offerStatus);

    if (offerStatus == OfferStatusEnum.ACCEPTED) {
      log.debug("Offer accepted for conversation ID: {}. Rejecting other pending offers.", id);
      rejectOtherPendingOffers(conversation.getRequestedService(), conversation.getId());

      conversation.getRequestedService().setStatus(RequestedServiceStatusEnum.INPROGRESS);
      log.debug("Requested service status updated to IN_PROGRESS for service ID: {}",
          conversation.getRequestedService().getId());
    }

    conversation.setOfferStatus(offerStatus);

    final var updatedConversation = conversationRepository.save(conversation);

    log.info("Offer status for conversation ID: {} successfully changed to: {}", updatedConversation.getId(),
        updatedConversation.getOfferStatus());
    return conversationMapper.conversationToConversationResponseDto(updatedConversation);
  }

  private void validateNewOffers(RequestedService requestedService, User serviceProvider,
      boolean alreadySubmittedAnOffer) {
    log.debug("Validating new offer for requested service ID: {} by service provider ID: {}", requestedService.getId(),
        serviceProvider.getId());

    if (requestedService.getStatus() != RequestedServiceStatusEnum.PENDING) {
      log.warn("Validation failed: Cannot make an offer for requested service ID {} because its status is not PENDING.",
          requestedService.getId());
      throw new ValidationException("Cannot make an offer for this requested service.");
    }

    if (serviceProvider.getId().equals(requestedService.getUser().getId())) {
      log.warn("Validation failed: Service provider ID {} is also the requester for service ID {}.",
          serviceProvider.getId(), requestedService.getId());
      throw new ValidationException("You cannot submit an offer for your own requested service.");
    }

    if (alreadySubmittedAnOffer) {
      log.warn("Validation failed: Service provider ID {} has already submitted a pending offer for service ID {}.",
          serviceProvider.getId(), requestedService.getId());
      throw new ValidationException("You have already submitted an offer for this requested service.");
    }

    log.debug("New offer validation successful for requested service ID: {}", requestedService.getId());
  }

  private void validateChangeOfferStatus(Conversation conversation, OfferStatusEnum offerStatus) {
    log.debug("Validating offer status change for conversation ID: {} to new status: {}", conversation.getId(),
        offerStatus);

    final var requestedService = conversation.getRequestedService();

    if (conversation.getOfferStatus() != OfferStatusEnum.PENDING) {
      log.warn("Validation failed: Offer status for conversation ID {} is not PENDING (current status: {}).",
          conversation.getId(), conversation.getOfferStatus());
      throw new ValidationException("Action allowed only when the offer status is PENDING.");
    }

    if (OfferStatusEnum.CANCELLED == offerStatus) {
      log.debug("Offer status set to CANCELLED for conversation ID {}. No further validation needed.",
          conversation.getId());
      return;
    }

    if (offerStatus == OfferStatusEnum.ACCEPTED
        && requestedService.getStatus() == RequestedServiceStatusEnum.INPROGRESS) {
      log.warn("Validation failed: Cannot accept offer for requested service ID {} because it's already IN_PROGRESS.",
          requestedService.getId());
      throw new ValidationException("Action allowed only when the requested service status is PENDING.");
    }

    validateNoOfferAcceptedForConversation(conversation.getRequestedService(),
        "It is not allowed to accept more than one offer for the same service request.");
    log.debug("Offer status change validation successful for conversation ID: {}", conversation.getId());
  }

  private void rejectOtherPendingOffers(RequestedService requestedService, Long conversationId) {
    log.debug("Rejecting other pending offers for requested service ID: {} excluding conversation ID: {}",
        requestedService.getId(), conversationId);
    requestedService.getConversations().stream()
        .filter(c -> c.getId() != conversationId)
        .filter(c -> c.getOfferStatus() == OfferStatusEnum.PENDING)
        .forEach(c -> {
          log.debug("Rejecting pending offer for conversation ID: {}", c.getId());
          this.changeOfferStatus(c.getId(), OfferStatusEnum.REJECTED);
        });
    log.debug("Finished rejecting other pending offers for requested service ID: {}", requestedService.getId());
  }

  private void validateNoOfferAcceptedForConversation(RequestedService requestedService, String message) {
    log.debug("Validating no other offer is accepted for requested service ID: {}", requestedService.getId());
    requestedService.getConversations().stream()
        .filter(c -> c.getOfferStatus() == OfferStatusEnum.ACCEPTED)
        .forEach(c -> {
          log.warn("Validation failed: An offer is already accepted for requested service ID {} (conversation ID: {}).",
              requestedService.getId(), c.getId());
          throw new ValidationException(message);
        });
    log.debug("No other accepted offer found for requested service ID: {}", requestedService.getId());
  }
}
