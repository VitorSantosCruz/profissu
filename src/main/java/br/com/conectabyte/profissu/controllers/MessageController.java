package br.com.conectabyte.profissu.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.services.MessageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {
  private final MessageService messageService;

  @PreAuthorize("@securityConversationService.ownershipCheck(#id) || @securityConversationService.requestedServiceOwner(#id) || @securityService.isAdmin()")
  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markMessageAsRead(@PathVariable Long id) {
    messageService.markAsRead(id);
    return ResponseEntity.accepted().build();
  }
}
