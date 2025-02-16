package br.com.conectabyte.profissu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import br.com.conectabyte.profissu.services.LoginService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/login")
@AllArgsConstructor
public class LoginController {
  private final LoginService loginService;

  @PostMapping
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto body) {
    return ResponseEntity.status(HttpStatus.CREATED).body(loginService.login(body));
  }
}
