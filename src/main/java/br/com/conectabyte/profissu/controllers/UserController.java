package br.com.conectabyte.profissu.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @Operation(summary = "Get user by ID", description = "Find a user by their ID and return user data.", responses = {
      @ApiResponse(responseCode = "200", description = "User successfully found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class))),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @GetMapping("/{id}")
  public ResponseEntity<UserResponseDto> findById(@PathVariable Long id) {
    return ResponseEntity.ok().body(this.userService.findById(id));
  }

  @Operation(summary = "Delete user profile by ID", description = "Soft deletes the user profile by the given ID.", responses = {
      @ApiResponse(responseCode = "202", description = "Accept delete request", content = @Content(mediaType = "application/json")),
      @ApiResponse(responseCode = "401", description = "Invalid or missing credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityService.isOwner(#id) || @securityService.isAdmin()")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    this.userService.deleteById(id);
    return ResponseEntity.accepted().build();
  }

  @Operation(summary = "Update user password", description = "Updates the password of a user identified by the given ID. Requires authentication.", responses = {
      @ApiResponse(responseCode = "204", description = "Password successfully updated"),
      @ApiResponse(responseCode = "400", description = "Malformed ID or missing parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "User does not have permission to update this password", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityService.isOwner(#id) || @securityService.isAdmin()")
  @PutMapping("/{id}/password")
  public ResponseEntity<Void> updatePassword(@PathVariable Long id,
      @Valid @RequestBody PasswordRequestDto passwordRequestDto) {
    this.userService.updatePassword(id, passwordRequestDto);
    return ResponseEntity.noContent().build();
  }
}
