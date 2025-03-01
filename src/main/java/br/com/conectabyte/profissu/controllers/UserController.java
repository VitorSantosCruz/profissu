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
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operations related to managing users")
public class UserController {
  private final UserService userService;

  @Operation(summary = "Retrieve user by ID", description = "Fetches a user's details using the provided ID. Requires authentication.", responses = {
      @ApiResponse(responseCode = "200", description = "User successfully retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Malformed ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "User does not have permission to access this resource", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "No user exists with the given ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @GetMapping("/{id}")
  public ResponseEntity<UserResponseDto> findById(@PathVariable Long id) {
    return ResponseEntity.ok().body(this.userService.findById(id));
  }

  @Operation(summary = "Soft delete user profile", description = "Marks the user profile as deleted (soft delete) using the provided ID. Requires authentication.", responses = {
      @ApiResponse(responseCode = "202", description = "The user profile will be soft deleted"),
      @ApiResponse(responseCode = "400", description = "Malformed ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "User does not have permission to delete this profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
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
