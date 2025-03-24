package br.com.conectabyte.profissu.exceptions;

public class RequestedServiceCancellationException extends RuntimeException {
  public RequestedServiceCancellationException(String message) {
    super(message);
  }
}
