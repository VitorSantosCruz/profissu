package br.com.conectabyte.profissu.controllers;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.dtos.response.MessageResponseDto;
import br.com.conectabyte.profissu.services.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Operations related to managing messages")
@Slf4j
public class MessageController {
  private final MessageService messageService;

  @Operation(summary = "List conversation messages", description = "Allows a participant of the conversation to retrieve the list of messages within an existing conversation, supporting pagination.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Messages successfully retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityConversationService.ownershipCheck(#conversationId) || @securityConversationService.isRequestedServiceOwner(#conversationId)")
  @GetMapping
  public Page<MessageResponseDto> listMessages(@RequestParam Long conversationId, @ParameterObject Pageable pageable) {
    log.debug("List messages request received. conversationId: {}, pageable: {}", conversationId, pageable);
    return this.messageService.listMessages(conversationId, pageable);
  }

  @Operation(summary = "Send a message in a conversation", description = "Allows the user who is part of the conversation to send a message within an existing conversation.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Message successfully sent", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PreAuthorize("@securityConversationService.ownershipCheck(#conversationId) || @securityConversationService.isRequestedServiceOwner(#conversationId)")
  @PostMapping
  public ResponseEntity<MessageResponseDto> sendMessage(@RequestParam Long conversationId,
      @Valid @RequestBody MessageRequestDto messageRequestDto) {
    log.debug("Send message request received. conversationId: {}, messageRequestDto: {}", conversationId, messageRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.messageService.sendMessage(conversationId, messageRequestDto));
  }

  @Operation(summary = "Mark a message as read", description = "Allows a participant of the conversation or an admin to mark a specific message as read to prevent repeated notifications.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Message successfully marked as read"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Message not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityMessageService.isMessageReceiver(#id)")
  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markMessageAsRead(@PathVariable Long id) {
    log.debug("Mark message as read request received. messageId: {}", id);
    messageService.markAsRead(id);
    return ResponseEntity.accepted().build();
  }
}
