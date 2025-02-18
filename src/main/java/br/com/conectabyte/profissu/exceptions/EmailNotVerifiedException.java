package br.com.conectabyte.profissu.exceptions;

public class EmailNotVerifiedException extends RuntimeException {
  public EmailNotVerifiedException(String message) {
    super(message);
  }
}