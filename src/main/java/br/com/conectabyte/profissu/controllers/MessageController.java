package br.com.conectabyte.profissu.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.services.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Operations related to managing messages")
public class MessageController {
  private final MessageService messageService;

  @Operation(summary = "Mark a message as read", description = "Allows a participant of the conversation or an admin to mark a specific message as read to prevent repeated notifications.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Message successfully marked as read"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Message not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityConversationService.ownershipCheck(#id) || @securityConversationService.requestedServiceOwner(#id) || @securityService.isAdmin()")
  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markMessageAsRead(@PathVariable Long id) {
    messageService.markAsRead(id);
    return ResponseEntity.accepted().build();
  }
}
