package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.ExceptionDto;
import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import br.com.conectabyte.profissu.dtos.PasswordRecoveryRequestDto;
import br.com.conectabyte.profissu.dtos.ResetPasswordRequestDto;
import br.com.conectabyte.profissu.dtos.ResetPasswordResponseDto;
import br.com.conectabyte.profissu.dtos.UserRequestDto;
import br.com.conectabyte.profissu.dtos.UserResponseDto;
import br.com.conectabyte.profissu.services.LoginService;
import br.com.conectabyte.profissu.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final LoginService loginService;
  private final UserService userService;

  @Operation(summary = "Authenticate user", description = "Validates user credentials and returns authentication details.", responses = {
      @ApiResponse(responseCode = "200", description = "User successfully authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto credentials) {
    return ResponseEntity.status(HttpStatus.CREATED).body(loginService.login(credentials));
  }

  @Operation(summary = "Register user", description = "Validates user data save and returns saved user data.", responses = {
      @ApiResponse(responseCode = "201", description = "User successfully created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PostMapping("/register")
  public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRequestDto user) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.save(user));
  }

  @Operation(summary = "Recover password", description = "Receives password recovery requests", responses = {
      @ApiResponse(responseCode = "201", description = "Password recovery request successfully received.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
  })
  @PostMapping("/password-recovery")
  public ResponseEntity<Void> recoverPassword(@Valid @RequestBody PasswordRecoveryRequestDto passwordRecoveryDto) {
    this.userService.recoverPassword(passwordRecoveryDto.email());
    return ResponseEntity.accepted().build();
  }

  @Operation(summary = "Reset password", description = "Receives password reset requests", responses = {
      @ApiResponse(responseCode = "200", description = "Password was successfully reset.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResetPasswordResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Password was not reset.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResetPasswordResponseDto.class)))
  })
  @PostMapping("/password-reset")
  public ResponseEntity<ResetPasswordResponseDto> resetPassword(
      @Valid @RequestBody ResetPasswordRequestDto resetPasswordRequestDto) {
    final var response = this.userService.resetPassword(resetPasswordRequestDto);
    return ResponseEntity.status(response.responseCode()).body(response);
  }
}
