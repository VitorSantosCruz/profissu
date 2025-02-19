package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.UserRequestDto;
import br.com.conectabyte.profissu.dtos.UserResponseDto;
import br.com.conectabyte.profissu.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserRequestDto user) {
    return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.registerUser(user));
  }
}
