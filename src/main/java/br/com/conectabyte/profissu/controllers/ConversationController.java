package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.services.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {
  private final ConversationService conversationService;

  @PostMapping
  public ResponseEntity<ConversationResponseDto> register(@Valid @RequestBody ConversationRequestDto conversationRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.conversationService.start(conversationRequestDto));
  }
}
