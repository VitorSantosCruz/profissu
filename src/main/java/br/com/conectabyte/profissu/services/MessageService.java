package br.com.conectabyte.profissu.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
  private final MessageRepository messageRepository;

  @Async
  public void markAsRead(Long id) {
    final var message = messageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

    message.setRead(true);
    messageRepository.save(message);
  }
}
