package br.com.conectabyte.profissu.exceptions;

public class ValidationException extends RuntimeException {
  public ValidationException(String message) {
    super(message);
  }
}
