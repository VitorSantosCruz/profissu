package br.com.conectabyte.profissu.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.repositories.MessageRepository;
import br.com.conectabyte.profissu.utils.MessageUtils;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock
  private MessageRepository messageRepository;

  @InjectMocks
  private MessageService messageService;

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
