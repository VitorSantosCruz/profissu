package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.ContactConfirmationRequestDto;
import br.com.conectabyte.profissu.dtos.request.EmailValueRequestDto;
import br.com.conectabyte.profissu.dtos.request.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.request.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.UserRequestDto;
import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.dtos.response.LoginResponseDto;
import br.com.conectabyte.profissu.dtos.response.MessageValueResponseDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.services.ContactService;
import br.com.conectabyte.profissu.services.LoginService;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.validators.groups.ValidatorGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Operations related to managing authentication")
public class AuthController {
  private final LoginService loginService;
  private final UserService userService;
  private final ContactService contactService;

  @Operation(summary = "Authenticate user", description = "Validates the provided user credentials and returns authentication details, including access tokens.", responses = {
      @ApiResponse(responseCode = "200", description = "User successfully authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto credentials) {
    return ResponseEntity.ok(loginService.login(credentials));
  }

  @Operation(summary = "Register new user", description = "Validates and saves the provided user data, creating a new user account.", responses = {
      @ApiResponse(responseCode = "201", description = "User successfully created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PostMapping("/register")
  public ResponseEntity<UserResponseDto> register(@Validated(ValidatorGroup.class) @RequestBody UserRequestDto user) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(user));
  }

  @Operation(summary = "Confirm user sign-up", description = "Validates and confirms a user's sign-up request.", responses = {
      @ApiResponse(responseCode = "200", description = "Sign-up successfully confirmed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageValueResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Sign-up confirmation failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageValueResponseDto.class)))
  })
  @PostMapping("/sign-up-confirmation")
  public ResponseEntity<MessageValueResponseDto> signUpConfirmation(
      @Valid @RequestBody ContactConfirmationRequestDto request) {
    final var response = this.contactService.contactConfirmation(request);
    return ResponseEntity.status(response.responseCode()).body(response);
  }

  @Operation(summary = "Resend sign-up confirmation email", description = "Triggers a request to resend the sign-up confirmation email.", responses = {
      @ApiResponse(responseCode = "202", description = "Confirmation email will be resent", content = @Content(mediaType = "application/json"))
  })
  @PostMapping("/sign-up-confirmation/resend")
  public ResponseEntity<Void> resendSignUpConfirmation(@Valid @RequestBody EmailValueRequestDto request) {
    this.userService.resendSignUpConfirmation(request);
    return ResponseEntity.accepted().build();
  }

  @Operation(summary = "Request password recovery", description = "Initiates the password recovery process by sending an email with recovery instructions.", responses = {
      @ApiResponse(responseCode = "202", description = "Password recovery email will be sent", content = @Content(mediaType = "application/json"))
  })
  @PostMapping("/password-recovery")
  public ResponseEntity<Void> recoverPassword(@Valid @RequestBody EmailValueRequestDto request) {
    this.userService.recoverPassword(request);
    return ResponseEntity.accepted().build();
  }

  @Operation(summary = "Reset user password", description = "Processes a password reset request and updates the user's password if valid.", responses = {
      @ApiResponse(responseCode = "200", description = "Password successfully reset", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageValueResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Password reset failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageValueResponseDto.class)))
  })
  @PostMapping("/password-reset")
  public ResponseEntity<MessageValueResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
    final var response = this.userService.resetPassword(request);
    return ResponseEntity.status(response.responseCode()).body(response);
  }
}
