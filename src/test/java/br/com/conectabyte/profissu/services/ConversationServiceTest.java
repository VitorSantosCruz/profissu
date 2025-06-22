package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.repositories.ConversationRepository;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Tests")
class ConversationServiceTest {
  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private JwtService jwtService;

  @Mock
  private UserService userService;

  @InjectMocks
  private ConversationService conversationService;

  @Test
  @DisplayName("Should create conversation when valid request")
  void shouldCreateConversationWhenValidRequest() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    when(requestedServiceService.findById(anyLong())).thenReturn(requestedService);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(anyLong())).thenReturn(serviceProvider);
    when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
    serviceProvider.setId(1L);

    final var savedConversation = conversationService.start(conversationRequestDto);

    assertNotNull(savedConversation);
    assertEquals(OfferStatusEnum.PENDING, savedConversation.offerStatus());
    verify(conversationRepository).save(any(Conversation.class));
  }

  @Test
  @DisplayName("Should throw exception when requested service is not pending")
  void shouldThrowExceptionWhenRequestedServiceIsNotPending() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");

    requestedService.setStatus(RequestedServiceStatusEnum.CANCELLED);

    when(requestedServiceService.findById(anyLong())).thenReturn(requestedService);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(anyLong())).thenReturn(serviceProvider);
    serviceProvider.setId(1L);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.start(conversationRequestDto));

    assertEquals("Cannot make an offer for this requested service.", exception.getMessage());
    verify(conversationRepository, never()).save(any(Conversation.class));
  }

  @Test
  @DisplayName("Should throw exception when service provider is requester")
  void shouldThrowExceptionWhenServiceProviderIsRequester() {
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(serviceProvider, AddressUtils.create(serviceProvider),
        List.of());
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");

    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);

    when(requestedServiceService.findById(anyLong())).thenReturn(requestedService);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(anyLong())).thenReturn(serviceProvider);
    serviceProvider.setId(1L);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.start(conversationRequestDto));

    assertEquals("You cannot submit an offer for your own requested service.", exception.getMessage());
    verify(conversationRepository, never()).save(any(Conversation.class));
  }

  @Test
  @DisplayName("Should throw exception when already submitted an offer")
  void shouldThrowExceptionWhenAlreadySubmittedAnOffer() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");

    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);
    serviceProvider.setId(1L);

    final var existingConversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    existingConversation.setOfferStatus(OfferStatusEnum.PENDING);
    requestedService.setConversations(List.of(existingConversation));

    when(requestedServiceService.findById(anyLong())).thenReturn(requestedService);
    when(jwtService.getClaims())
        .thenReturn(Optional.of(new HashMap<>(Map.of("sub", serviceProvider.getId().toString()))));
    when(userService.findById(anyLong())).thenReturn(serviceProvider);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.start(conversationRequestDto));

    assertEquals("You have already submitted an offer for this requested service.", exception.getMessage());
    verify(conversationRepository, never()).save(any(Conversation.class));
  }

  @Test
  @DisplayName("Should create conversation successfully when other offers are not PENDING")
  void shouldCreateConversationSuccessfullyWhenOtherOffersAreNotPending() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);
    serviceProvider.setId(1L);

    final var acceptedOffer = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());
    acceptedOffer.setOfferStatus(OfferStatusEnum.ACCEPTED);
    final var rejectedOffer = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());
    rejectedOffer.setOfferStatus(OfferStatusEnum.REJECTED);
    requestedService.setConversations(List.of(acceptedOffer, rejectedOffer));

    when(requestedServiceService.findById(anyLong())).thenReturn(requestedService);
    when(jwtService.getClaims())
        .thenReturn(Optional.of(new HashMap<>(Map.of("sub", serviceProvider.getId().toString()))));
    when(userService.findById(anyLong())).thenReturn(serviceProvider);
    when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

    final var savedConversation = conversationService.start(conversationRequestDto);

    assertNotNull(savedConversation);
    assertEquals(OfferStatusEnum.PENDING, savedConversation.offerStatus());
    verify(conversationRepository).save(any(Conversation.class));
  }

  @Test
  @DisplayName("Should return conversation when found by id")
  void shouldReturnConversationWhenFoundById() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());
    conversation.setId(1L);

    when(conversationRepository.findById(anyLong())).thenReturn(Optional.of(conversation));

    final var result = conversationService.findById(conversation.getId());

    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(conversationRepository).findById(conversation.getId());
  }

  @Test
  @DisplayName("Should throw when conversation not found by id")
  void shouldThrowWhenConversationNotFoundById() {
    when(conversationRepository.findById(anyLong())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.findById(1L));

    assertEquals("Conversation not found.", exception.getMessage());
    verify(conversationRepository).findById(1L);
  }

  @Test
  @DisplayName("Should find current user conversations successfully")
  void shouldFindCurrentUserConversationsSuccessfully() {
    final Long userId = 1L;
    final var pageable = PageRequest.of(0, 10);
    final var conversation = ConversationUtils.create(UserUtils.create(), UserUtils.create(), null, List.of());
    final var conversationPage = new PageImpl<>(List.of(conversation), pageable, 1);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", userId.toString()))));
    when(conversationRepository.findByUserId(userId, pageable)).thenReturn(conversationPage);

    final var result = conversationService.findCurrentUserConversations(pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(conversation.getId(), result.getContent().get(0).id());
    verify(jwtService).getClaims();
    verify(conversationRepository).findByUserId(userId, pageable);
  }

  @Test
  @DisplayName("Should return empty page when no conversations for user found")
  void shouldReturnEmptyPageWhenNoConversationsForUserFound() {
    final Long userId = 0L;
    final var pageable = PageRequest.of(0, 10);
    final Page<Conversation> emptyPage = Page.empty(pageable);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", userId.toString()))));
    when(conversationRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

    final var result = conversationService.findCurrentUserConversations(pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    assertTrue(result.isEmpty());
    verify(jwtService).getClaims();
    verify(conversationRepository).findByUserId(userId, pageable);
  }

  @Test
  @DisplayName("Should cancel pending conversation")
  void shouldCancelPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversation.setId(1L);
    conversation.setOfferStatus(OfferStatusEnum.PENDING);

    when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
    when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.changeOfferStatus(conversation.getId(), OfferStatusEnum.CANCELLED);

    assertNotNull(response);
    assertEquals(OfferStatusEnum.CANCELLED, response.offerStatus());
    assertEquals(OfferStatusEnum.CANCELLED, conversation.getOfferStatus());
    verify(conversationRepository).save(conversation);
  }

  @Test
  @DisplayName("Should throw if conversation does not exist on cancel")
  void shouldThrowIfConversationDoesNotExistOnCancel() {
    when(conversationRepository.findById(anyLong())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.changeOfferStatus(1L, OfferStatusEnum.CANCELLED));

    assertEquals("Conversation not found.", exception.getMessage());
    verify(conversationRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw when canceling non-pending conversation")
  void shouldThrowWhenCancelingNonPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversation.setId(1L);
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.changeOfferStatus(conversation.getId(), OfferStatusEnum.CANCELLED));

    assertEquals("Action allowed only when the offer status is PENDING.", exception.getMessage());
    verify(conversationRepository).findById(conversation.getId());
  }

  @Test
  @DisplayName("Should accept pending conversation and reject other pending offers")
  void shouldAcceptPendingConversationAndRejectOthers() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    requestedService.setId(100L);
    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);

    final var conversationToAccept = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversationToAccept.setId(1L);
    conversationToAccept.setOfferStatus(OfferStatusEnum.PENDING);

    final var otherPendingConversation = ConversationUtils.create(requester, UserUtils.create(), requestedService,
        List.of());
    otherPendingConversation.setId(2L);
    otherPendingConversation.setOfferStatus(OfferStatusEnum.PENDING);

    final var alreadyRejectedConversation = ConversationUtils.create(requester, UserUtils.create(), requestedService,
        List.of());
    alreadyRejectedConversation.setId(3L);
    alreadyRejectedConversation.setOfferStatus(OfferStatusEnum.REJECTED);

    requestedService
        .setConversations(List.of(conversationToAccept, otherPendingConversation, alreadyRejectedConversation));

    when(conversationRepository.findById(conversationToAccept.getId())).thenReturn(Optional.of(conversationToAccept));
    when(conversationRepository.findById(otherPendingConversation.getId()))
        .thenReturn(Optional.of(otherPendingConversation));
    when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.changeOfferStatus(conversationToAccept.getId(), OfferStatusEnum.ACCEPTED);

    assertNotNull(response);
    assertEquals(OfferStatusEnum.ACCEPTED, response.offerStatus());
    assertEquals(RequestedServiceStatusEnum.INPROGRESS, requestedService.getStatus());

    verify(conversationRepository).save(conversationToAccept);
    assertEquals(OfferStatusEnum.ACCEPTED, conversationToAccept.getOfferStatus());

    verify(conversationRepository).save(otherPendingConversation);
    assertEquals(OfferStatusEnum.REJECTED, otherPendingConversation.getOfferStatus());

    verify(conversationRepository, never()).save(alreadyRejectedConversation);
  }

  @Test
  @DisplayName("Should throw if conversation does not exist on accept")
  void shouldThrowIfConversationDoesNotExistOnAccept() {
    when(conversationRepository.findById(anyLong())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.changeOfferStatus(1L, OfferStatusEnum.ACCEPTED));

    assertEquals("Conversation not found.", exception.getMessage());
    verify(conversationRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw when accepting non-pending conversation")
  void shouldThrowWhenAcceptingNonPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversation.setId(1L);
    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);

    when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.changeOfferStatus(conversation.getId(), OfferStatusEnum.ACCEPTED));

    assertEquals("Action allowed only when the offer status is PENDING.", exception.getMessage());
    verify(conversationRepository).findById(conversation.getId());
  }

  @Test
  @DisplayName("Should reject pending conversation")
  void shouldRejectPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversation.setId(1L);
    conversation.setOfferStatus(OfferStatusEnum.PENDING);

    when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
    when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.changeOfferStatus(conversation.getId(), OfferStatusEnum.REJECTED);

    assertNotNull(response);
    assertEquals(OfferStatusEnum.REJECTED, response.offerStatus());
    assertEquals(OfferStatusEnum.REJECTED, conversation.getOfferStatus());
    verify(conversationRepository).save(conversation);
  }

  @Test
  @DisplayName("Should throw if conversation does not exist on reject")
  void shouldThrowIfConversationDoesNotExistOnReject() {
    when(conversationRepository.findById(anyLong())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.changeOfferStatus(1L, OfferStatusEnum.REJECTED));

    assertEquals("Conversation not found.", exception.getMessage());
    verify(conversationRepository).findById(1L);
  }

  @Test
  @DisplayName("Should throw when rejecting non-pending conversation")
  void shouldThrowWhenRejectingNonPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversation.setId(1L);
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.changeOfferStatus(conversation.getId(), OfferStatusEnum.REJECTED));

    assertEquals("Action allowed only when the offer status is PENDING.", exception.getMessage());
    verify(conversationRepository).findById(conversation.getId());
  }

  @Test
  @DisplayName("Should throw ValidationException when accepting offer and requested service is already IN_PROGRESS")
  void shouldThrowWhenAcceptingOfferAndServiceAlreadyInProgress() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    requestedService.setId(100L);
    requestedService.setStatus(RequestedServiceStatusEnum.INPROGRESS);

    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversation.setId(1L);
    conversation.setOfferStatus(OfferStatusEnum.PENDING);

    when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.changeOfferStatus(conversation.getId(), OfferStatusEnum.ACCEPTED));

    assertEquals("Action allowed only when the requested service status is PENDING.", exception.getMessage());
    verify(conversationRepository).findById(conversation.getId());
  }

  @Test
  @DisplayName("Should throw ValidationException when accepting offer and another offer is already ACCEPTED")
  void shouldThrowWhenAcceptingOfferAndAnotherOfferIsAlreadyAccepted() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester), List.of());
    requestedService.setId(100L);
    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);

    final var conversationToAccept = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    conversationToAccept.setId(1L);
    conversationToAccept.setOfferStatus(OfferStatusEnum.PENDING);

    final var alreadyAcceptedConversation = ConversationUtils.create(requester, UserUtils.create(), requestedService,
        List.of());
    alreadyAcceptedConversation.setId(2L);
    alreadyAcceptedConversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    requestedService.setConversations(List.of(conversationToAccept, alreadyAcceptedConversation));

    when(conversationRepository.findById(conversationToAccept.getId())).thenReturn(Optional.of(conversationToAccept));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.changeOfferStatus(conversationToAccept.getId(), OfferStatusEnum.ACCEPTED));

    assertEquals("It is not allowed to accept more than one offer for the same service request.",
        exception.getMessage());
    verify(conversationRepository).findById(conversationToAccept.getId());
  }
}
