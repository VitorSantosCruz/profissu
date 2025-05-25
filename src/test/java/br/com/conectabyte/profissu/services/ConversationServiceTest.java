package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ConversationMapper;
import br.com.conectabyte.profissu.repositories.ConversationRepository;
import br.com.conectabyte.profissu.repositories.MessageRepository;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private JwtService jwtService;

  @Mock
  private UserService userService;

  @Mock
  private MessageRepository messageRepository;

  @Mock
  private SimpMessagingTemplate simpMessagingTemplate;

  @InjectMocks
  private ConversationService conversationService;

  @Test
  void shouldCreateConversationWhenValidRequest() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(serviceProvider);
    when(conversationRepository.save(any())).thenReturn(conversation);
    serviceProvider.setId(1L);

    final var savedConversation = conversationService.start(conversationRequestDto);

    assertNotNull(savedConversation);
    assertEquals(OfferStatusEnum.PENDING, savedConversation.offerStatus());
    verify(conversationRepository).save(any(Conversation.class));
  }

  @Test
  void shouldThrowExceptionWhenRequestedServiceIsNotPending() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(serviceProvider);
    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);
    serviceProvider.setId(1L);
    requestedService.setStatus(RequestedServiceStatusEnum.CANCELLED);
    requestedService.setConversations(List.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.start(conversationRequestDto));

    assertEquals("Cannot make an offer for this requested service.", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenServiceProviderIsRequester() {
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(serviceProvider, AddressUtils.create(serviceProvider));
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");

    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(serviceProvider);
    serviceProvider.setId(1L);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.start(conversationRequestDto));

    assertEquals("You cannot submit an offer for your own requested service.", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenAlreadySubmittedAnOffer() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(serviceProvider);
    requestedService.setConversations(List.of(conversation));
    serviceProvider.setId(1L);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.start(conversationRequestDto));

    assertEquals("You have already submitted an offer for this requested service.", exception.getMessage());
  }

  @Test
  void shouldReturnConversationWhenFoundById() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());
    conversation.setId(1L);

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));

    final var result = conversationService.findById(any());

    assertNotNull(result);
    assertEquals(1L, result.getId());
  }

  @Test
  void shouldThrowWhenConversationNotFoundById() {
    when(conversationRepository.findById(any())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.findById(any()));

    assertEquals("Conversation not found.", exception.getMessage());
  }

  @Test
  void shouldCancelPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    conversation.setId(1L);
    serviceProvider.setConversationsAsAServiceProvider(List.of(conversation));

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));
    when(conversationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.cancel(1L);

    assertNotNull(response);
    assertEquals(OfferStatusEnum.CANCELLED, response.offerStatus());
  }

  @Test
  void shouldThrowIfConversationDoesNotExistOnCancel() {
    final var serviceProvider = UserUtils.create();

    serviceProvider.setConversationsAsAServiceProvider(List.of());

    when(conversationRepository.findById(any())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.cancel(1L));

    assertEquals("Conversation not found.", exception.getMessage());
  }

  @Test
  void shouldThrowWhenCancelingNonExistentConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester));
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.cancel(1L));

    assertEquals("Conversation cannot be canceled.", exception.getMessage());
  }

  @Test
  void shouldFindRequestedServiceByUserIdSuccessfully() {
    final var pageable = PageRequest.of(0, 10);
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester));
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());
    final var conversationResponseDto = ConversationMapper.INSTANCE
        .conversationToConversationResponseDto(conversation);
    final var conversationPage = new PageImpl<>(List.of(conversation), pageable, 1);
    final var expectedResponsePage = new PageImpl<>(List.of(conversationResponseDto), pageable, 1);

    when(conversationRepository.findByUserId(any(), any())).thenReturn(conversationPage);

    final var result = conversationService.findByUserId(1L, pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(expectedResponsePage, result);
  }

  @Test
  void shouldReturnEmptyPageWhenNoRequestedServiceForUserFound() {
    final var pageable = PageRequest.of(0, 10);
    final Page<Conversation> emptyPage = Page.empty(pageable);
    final var expectedEmptyResponsePage = Page.empty(pageable);

    when(conversationRepository.findByUserId(0L, pageable)).thenReturn(emptyPage);

    final var result = conversationService.findByUserId(0L, pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    assertEquals(expectedEmptyResponsePage, result);
  }

  @Test
  void shouldAcceptPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    conversation.setId(1L);
    serviceProvider.setConversationsAsAServiceProvider(List.of(conversation));

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));
    when(conversationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.ACCEPTED);

    assertNotNull(response);
    assertEquals(OfferStatusEnum.ACCEPTED, response.offerStatus());
  }

  @Test
  void shouldThrowIfConversationDoesNotExistOnAccept() {
    final var serviceProvider = UserUtils.create();

    serviceProvider.setConversationsAsAServiceProvider(List.of());

    when(conversationRepository.findById(any())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.ACCEPTED));

    assertEquals("Conversation not found.", exception.getMessage());
  }

  @Test
  void shouldThrowWhenAcceptingNonPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester));
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.ACCEPTED));

    assertEquals("Conversation cannot be accepted/rejected.", exception.getMessage());
  }

  @Test
  void shouldRejectPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    conversation.setId(1L);
    serviceProvider.setConversationsAsAServiceProvider(List.of(conversation));

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));
    when(conversationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.REJECTED);

    assertNotNull(response);
    assertEquals(OfferStatusEnum.REJECTED, response.offerStatus());
  }

  @Test
  void shouldThrowIfConversationDoesNotExistOnReject() {
    final var serviceProvider = UserUtils.create();

    serviceProvider.setConversationsAsAServiceProvider(List.of());

    when(conversationRepository.findById(any())).thenReturn(Optional.empty());

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.REJECTED));

    assertEquals("Conversation not found.", exception.getMessage());
  }

  @Test
  void shouldThrowWhenRejectingNonPendingConversation() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester));
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.REJECTED));

    assertEquals("Conversation cannot be accepted/rejected.", exception.getMessage());
  }

  @Test
  void shouldThrowWhenAcceptOrRejectConversationWithInvalidStatus() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester));
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));

    ValidationException exception1 = assertThrows(ValidationException.class,
        () -> conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.CANCELLED));

    ValidationException exception2 = assertThrows(ValidationException.class,
        () -> conversationService.acceptOrRejectOffer(1L, OfferStatusEnum.PENDING));

    assertEquals("Offer status is invalid.", exception1.getMessage());
    assertEquals("Offer status is invalid.", exception2.getMessage());
  }

  @Test
  void shouldSendMessageSuccessfully() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    final var messageRequestDto = new MessageRequestDto("Teste");

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(user);
    when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.sendMessage(1L, messageRequestDto);

    assertNotNull(response);
    verify(messageRepository).save(any());
    verify(simpMessagingTemplate).convertAndSend(any(), any(Object.class));
  }

  @Test
  void shouldThrowWhenConversationNotFound() {
    when(conversationRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> conversationService.sendMessage(1L, new MessageRequestDto("Hi")));
  }

  @Test
  void shouldThrowWhenUserNotFound() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    when(conversationRepository.findById(any())).thenReturn(Optional.of(conversation));
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenThrow(new ResourceNotFoundException("User not found."));

    assertThrows(ResourceNotFoundException.class,
        () -> conversationService.sendMessage(1L, new MessageRequestDto("Teste")));
  }
}
