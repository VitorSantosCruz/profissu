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
import br.com.conectabyte.profissu.services.ContactService;
import br.com.conectabyte.profissu.validators.groups.ValidatorGroup;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Operations related to managing contacts")
public class ContactController {
  private final ContactService contactService;

  @PreAuthorize("@securityService.isOwner(#userId) || @securityService.isAdmin()")
  @PostMapping("/{userId}")
  public ResponseEntity<ContactResponseDto> register(@PathVariable Long userId,
      @Validated(ValidatorGroup.class) @RequestBody ContactRequestDto contactRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.contactService.register(userId, contactRequestDto));
  }

  @PreAuthorize("@securityService.isOwnerOfContact(#id) || @securityService.isAdmin()")
  @PutMapping("/{id}")
  public ResponseEntity<ContactResponseDto> update(@PathVariable Long id,
      @Validated @RequestBody ContactRequestDto contactRequestDto) {
    return ResponseEntity.ok().body(this.contactService.update(id, contactRequestDto));
  }
}
