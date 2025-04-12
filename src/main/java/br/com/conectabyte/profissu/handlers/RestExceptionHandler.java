package br.com.conectabyte.profissu.handlers;

import java.util.ArrayList;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import br.com.conectabyte.profissu.exceptions.RequestedServiceCancellationException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ExceptionDto> validationExceptionHandler(MethodArgumentNotValidException e) {
    log.error("Error: {}", e.getMessage());
      final var errors = new ArrayList<String>();
    e.getAllErrors().forEach(err -> errors.add(err.getDefaultMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionDto("All fields must be valid", errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ExceptionDto> malformedExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionDto("Malformed json", null));
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<ExceptionDto> propertyReferenceExceptionHandler(PropertyReferenceException e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ExceptionDto("No property '" + e.getPropertyName() + "' found", null));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ExceptionDto> validationExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionDto(e.getMessage(), null));
  }

  @ExceptionHandler(RequestedServiceCancellationException.class)
  public ResponseEntity<ExceptionDto> requestedServiceCancellationExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ExceptionDto(e.getMessage(), null));
  }

  @ExceptionHandler({ BadCredentialsException.class, EmailNotVerifiedException.class })
  public ResponseEntity<ExceptionDto> credentialsExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ExceptionDto(e.getMessage(), null));
  }

  @ExceptionHandler({ NoResourceFoundException.class, ResourceNotFoundException.class })
  public ResponseEntity<ExceptionDto> notFoundExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    final var messageError = e instanceof ResourceNotFoundException ? e.getMessage() : "Resource not found";

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ExceptionDto(messageError, null));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ExceptionDto> handleAccessDeniedException(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ExceptionDto("Access denied.", null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionDto> genericExceptionHandler(Exception e) {
    log.error("Error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionDto("Server error", null));
  }
}
