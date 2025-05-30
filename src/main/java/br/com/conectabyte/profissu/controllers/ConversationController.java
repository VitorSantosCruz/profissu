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
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.services.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Operations related to managing service offers and conversations")
public class ConversationController {
  private final ConversationService conversationService;

  @Operation(summary = "Retrieve user conversations", description = "Fetches a paginated list of conversations of the current user.", responses = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved conversations", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @GetMapping
  public Page<ConversationResponseDto> findCurrentUserConversations(@ParameterObject Pageable pageable) {
    return conversationService.findCurrentUserConversations(pageable);
  }

  @Operation(summary = "Make an offer for a requested service", description = "Allows a user to make an offer by opening a conversation related to a requested service.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Offer successfully created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConversationResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Requested service not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PostMapping
  public ResponseEntity<ConversationResponseDto> start(
      @Valid @RequestBody ConversationRequestDto conversationRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.conversationService.start(conversationRequestDto));
  }

  @Operation(summary = "Cancel a service offer", description = "Allows the user who created the conversation or an admin to cancel an existing offer.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Offer successfully canceled", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConversationResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PreAuthorize("@securityConversationService.ownershipCheck(#id) || @securityService.isAdmin()")
  @PatchMapping("/{id}")
  public ResponseEntity<ConversationResponseDto> cancel(@PathVariable Long id) {
    return ResponseEntity.ok(this.conversationService.cancel(id));
  }

  @Operation(summary = "Accept or reject a service offer", description = "Allows the user who owns the requested service to accept or reject a pending offer.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Offer successfully updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConversationResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or offer status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PreAuthorize("@securityConversationService.isRequestedServiceOwner(#id)")
  @PatchMapping("/{id}/{offerStatus}")
  public ResponseEntity<ConversationResponseDto> acceptOrRejectOffer(@PathVariable Long id,
      @PathVariable OfferStatusEnum offerStatus) {
    return ResponseEntity.ok(this.conversationService.acceptOrRejectOffer(id, offerStatus));
  }
}
