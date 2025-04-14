package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
    when(jwtService.getClaims()).thenReturn(Optional.of(new java.util.HashMap<>(java.util.Map.of("sub", "1"))));
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
    when(jwtService.getClaims()).thenReturn(Optional.of(new java.util.HashMap<>(java.util.Map.of("sub", "1"))));
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
    when(jwtService.getClaims()).thenReturn(Optional.of(new java.util.HashMap<>(java.util.Map.of("sub", "1"))));
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
    when(jwtService.getClaims()).thenReturn(Optional.of(new java.util.HashMap<>(java.util.Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(serviceProvider);
    requestedService.setConversations(List.of(conversation));
    serviceProvider.setId(1L);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> conversationService.start(conversationRequestDto));

    assertEquals("You have already submitted an offer for this requested service.", exception.getMessage());
  }
}
