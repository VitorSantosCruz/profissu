package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.ContactRequestDto;
import br.com.conectabyte.profissu.dtos.response.ContactResponseDto;
import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.services.ContactService;
import br.com.conectabyte.profissu.validators.groups.ValidatorGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Operations related to managing contacts")
public class ContactController {
  private final ContactService contactService;

  @Operation(summary = "Register contact", description = "Registers a new contact for the specified user.", responses = {
      @ApiResponse(responseCode = "201", description = "Contact successfully registered", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContactResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PostMapping
  public ResponseEntity<ContactResponseDto> register(
      @Validated(ValidatorGroup.class) @RequestBody ContactRequestDto contactRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.contactService.register(contactRequestDto));
  }

  @Operation(summary = "Update contact", description = "Updates an existing contact. Only the owner of the contact can perform this operation.", responses = {
      @ApiResponse(responseCode = "200", description = "Contact successfully updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContactResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Contact not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityContactService.ownershipCheck(#id)")
  @PutMapping("/{id}")
  public ResponseEntity<ContactResponseDto> update(@PathVariable Long id,
      @Validated @RequestBody ContactRequestDto contactRequestDto) {
    return ResponseEntity.ok().body(this.contactService.update(id, contactRequestDto));
  }
}
