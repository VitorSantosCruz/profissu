package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.ExceptionDto;
import br.com.conectabyte.profissu.dtos.UserRequestDto;
import br.com.conectabyte.profissu.dtos.UserResponseDto;
import br.com.conectabyte.profissu.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {
  private final UserService userService;

  @Operation(summary = "Register user", description = "Validates user data save and returns saved user data.", responses = {
      @ApiResponse(responseCode = "201", description = "User successfully created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PostMapping
  public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRequestDto user) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.save(user));
  }
}
