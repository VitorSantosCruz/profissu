package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.AddressRequestDto;
import br.com.conectabyte.profissu.dtos.response.AddressResponseDto;
import br.com.conectabyte.profissu.services.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Operations related to managing addresses")
public class AddressController {
  private final AddressService addressService;

  @PreAuthorize("@securityService.isOwner(#userId) || @securityService.isAdmin()")
  @PostMapping("/{userId}")
  public ResponseEntity<AddressResponseDto> register(@PathVariable Long userId,
      @Valid @RequestBody AddressRequestDto addressRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.addressService.register(userId, addressRequestDto));
  }

  @PreAuthorize("@securityService.isOwnerOfAddress(#id) || @securityService.isAdmin()")
  @PutMapping("/{id}")
  public ResponseEntity<AddressResponseDto> update(@PathVariable Long id,
      @Valid @RequestBody AddressRequestDto addressRequestDto) {
    return ResponseEntity.ok().body(this.addressService.update(id, addressRequestDto));
  }
}
