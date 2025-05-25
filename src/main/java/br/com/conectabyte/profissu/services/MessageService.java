package br.com.conectabyte.profissu.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
  private final MessageRepository messageRepository;

  public Message findById(Long id) {
    final var optionalMessage = messageRepository.findById(id);
    final var message = optionalMessage.orElseThrow(() -> new ResourceNotFoundException("Message not found."));

    return message;
  }

  @Async
  public void markAsRead(Long id) {
    final var message = this.findById(id);

    message.setRead(true);
    messageRepository.save(message);
  }
}
