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

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(serviceProvider);
    when(conversationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = conversationService.cancel(1L);

    assertNotNull(response);
    assertEquals(OfferStatusEnum.CANCELLED, response.offerStatus());
  }

  @Test
  void shouldThrowWhenConversationNotFound() {
    final var serviceProvider = UserUtils.create();

    serviceProvider.setConversationsAsAServiceProvider(List.of());

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(serviceProvider);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> conversationService.cancel(1L));

    assertEquals("Conversation not found.", exception.getMessage());
  }

  @Test
  void shouldThrowWhenConversationIsNotPending() {
    final var serviceProvider = UserUtils.create();
    final var requester = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(requester, AddressUtils.create(requester));
    final var conversation = ConversationUtils.create(requester, serviceProvider, requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);
    conversation.setId(1L);
    serviceProvider.setConversationsAsAServiceProvider(List.of(conversation));

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(1L)).thenReturn(serviceProvider);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.cancel(1L));

    assertEquals("Conversation cannot be canceled.", exception.getMessage());
  }
}
