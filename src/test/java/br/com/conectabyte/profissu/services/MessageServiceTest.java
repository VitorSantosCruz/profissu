package br.com.conectabyte.profissu.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.MessageMapper;
import br.com.conectabyte.profissu.repositories.MessageRepository;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.MessageUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock
  private MessageRepository messageRepository;

  @Mock
  private ConversationService conversationService;

  @Mock
  private JwtService jwtService;

  @Mock
  private UserService userService;

  @Mock
  private SimpMessagingTemplate simpMessagingTemplate;

  @InjectMocks
  private MessageService messageService;

  @Test
  void shouldSendMessageSuccessfullyWhenStatusIdPending() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    final var messageRequestDto = new MessageRequestDto("Test");

    when(conversationService.findById(any())).thenReturn(conversation);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(user);
    when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = messageService.sendMessage(1L, messageRequestDto);

    assertNotNull(response);
    verify(messageRepository).save(any());
    verify(simpMessagingTemplate).convertAndSend(any(), any(Object.class));
  }

  @Test
  void shouldSendMessageSuccessfullyWhenStatusIdAccepted() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    final var messageRequestDto = new MessageRequestDto("Test");

    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    when(conversationService.findById(any())).thenReturn(conversation);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(user);
    when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = messageService.sendMessage(1L, messageRequestDto);

    assertNotNull(response);
    verify(messageRepository).save(any());
    verify(simpMessagingTemplate).convertAndSend(any(), any(Object.class));
  }

  @Test
  void shouldThrowValidationExceptionWhenSendingMessageWithInvalidOfferStatus() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);

    final var messageRequestDto = new MessageRequestDto("Test message");

    when(conversationService.findById(any())).thenReturn(conversation);

    ValidationException exception = assertThrows(ValidationException.class,
        () -> messageService.sendMessage(1L, messageRequestDto));

    assertEquals("This offer has already been canceled or rejected.", exception.getMessage());
  }

  @Test
  void shouldThrowWhenConversationNotFound() {
    when(conversationService.findById(any())).thenThrow(new ResourceNotFoundException("Conversation not found."));

    assertThrows(ResourceNotFoundException.class,
        () -> messageService.sendMessage(1L, new MessageRequestDto("Hi")));
  }

  @Test
  void shouldThrowWhenUserNotFound() {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    when(conversationService.findById(any())).thenReturn(conversation);
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenThrow(new ResourceNotFoundException("User not found."));

    assertThrows(ResourceNotFoundException.class,
        () -> messageService.sendMessage(1L, new MessageRequestDto("Test")));
  }

  @Test
  void shouldListMessagesSuccessfully() {
    final var conversationId = 1L;
    final var pageable = PageRequest.of(0, 10);
    final var message = MessageUtils.create(null, null);
    final var messagePage = new PageImpl<>(List.of(message), pageable, 1);
    final var messageResponseDtoPage = MessageMapper.INSTANCE.messagePageToMessageResponseDtoPage(messagePage);

    when(messageRepository.listMessages(conversationId, pageable)).thenReturn(messagePage);

    final var result = messageService.listMessages(conversationId, pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(messageResponseDtoPage, result);

    verify(messageRepository).listMessages(conversationId, pageable);
  }

  @Test
  void shouldFindMessageByIdWhenExists() {
    final var message = MessageUtils.create(null, null);
    when(messageRepository.findById(any())).thenReturn(Optional.of(message));

    Message foundMessage = messageService.findById(1L);

    assert (foundMessage).equals(message);
    verify(messageRepository).findById(any());
  }

  @Test
  void shouldThrowResourceNotFoundWhenMessageDoesNotExist() {
    when(messageRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> messageService.findById(1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Message not found.");

    verify(messageRepository).findById(any());
  }

  @Test
  void shouldMarkMessageAsReadWhenExists() {
    final var message = MessageUtils.create(null, null);
    when(messageRepository.findById(any())).thenReturn(Optional.of(message));
    when(messageRepository.save(any(Message.class))).thenReturn(message);

    messageService.markAsRead(1L);

    assert (message.isRead());
    verify(messageRepository).findById(any());
    verify(messageRepository).save(message);
  }
}
