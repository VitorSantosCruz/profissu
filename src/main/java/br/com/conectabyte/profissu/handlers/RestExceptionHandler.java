package br.com.conectabyte.profissu.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.conectabyte.profissu.dtos.ExceptionDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ExceptionDto> credentialsExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ExceptionDto("bad.credentials.exception"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionDto> genericExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionDto("server.exception"));
  }
}
