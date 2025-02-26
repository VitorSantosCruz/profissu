package br.com.conectabyte.profissu.handlers;

import java.util.ArrayList;

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
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler({ MethodArgumentNotValidException.class })
  public ResponseEntity<ExceptionDto> validationExceptionHandler(MethodArgumentNotValidException e) {
    log.error("Error: {}", e.getMessage());
    val errors = new ArrayList<String>();
    e.getAllErrors().forEach(err -> errors.add(err.getDefaultMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionDto("All fields must be valid", errors));
  }

  @ExceptionHandler({ HttpMessageNotReadableException.class })
  public ResponseEntity<ExceptionDto> malformedExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionDto("Malformed json", null));
  }

  @ExceptionHandler({ BadCredentialsException.class, EmailNotVerifiedException.class })
  public ResponseEntity<ExceptionDto> credentialsExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ExceptionDto(e.getMessage(), null));
  }

  @ExceptionHandler({ NoResourceFoundException.class, ResourceNotFoundException.class })
  public ResponseEntity<ExceptionDto> notFoundExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    if (e instanceof ResourceNotFoundException) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ExceptionDto(e.getMessage(), null));
    }

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ExceptionDto("Resource not found", null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionDto> genericExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionDto("Server error", null));
  }
}
