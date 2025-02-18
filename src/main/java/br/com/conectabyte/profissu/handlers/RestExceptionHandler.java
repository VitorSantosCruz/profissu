package br.com.conectabyte.profissu.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.com.conectabyte.profissu.dtos.ExceptionDto;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class })
  public ResponseEntity<ExceptionDto> validationExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionDto("bad.request.exception"));
  }

  @ExceptionHandler({ BadCredentialsException.class, EmailNotVerifiedException.class })
  public ResponseEntity<ExceptionDto> credentialsExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ExceptionDto(
        e instanceof EmailNotVerifiedException ? "email.is.not.verified" : "bad.credentials.exception"));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ExceptionDto> notFoundExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ExceptionDto("resource.not.found.exception"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionDto> genericExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionDto("server.exception"));
  }
}
