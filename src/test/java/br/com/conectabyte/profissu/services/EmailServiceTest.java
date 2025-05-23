package br.com.conectabyte.profissu.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
  @Mock
  private JavaMailSender javaMailSender;

  @Mock
  private TemplateEngine templateEngine;

  @InjectMocks
  private EmailService emailService;

  @Test
  void shouldSendPasswordRecoveryEmailSuccessfully() throws MessagingException {
    final var htmlContent = "<html><body>Reset Code: 123456</body></html>";
    final var mimeMessage = mock(MimeMessage.class);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(any(String.class), any(Context.class))).thenReturn(htmlContent);

    emailService.sendPasswordRecoveryEmail("test@conectabyte.com.br", "CODE");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);
    verify(templateEngine, times(1)).process(any(String.class), any(Context.class));
  }

  @Test
  void shouldLogErrorSendPasswordRecoveryEmailWhenMessagingExceptionIsThrown() throws Exception {
    final var mimeMessage = mock(MimeMessage.class);

    doAnswer(invocation -> {
      throw new MessagingException("Simulated MessagingException");
    }).when(javaMailSender).createMimeMessage();

    emailService.sendPasswordRecoveryEmail("test@conectabyte.com.br", "CODE");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(0)).send(mimeMessage);
    verify(templateEngine, times(0)).process(any(String.class), any(Context.class));
  }

  @Test
  void shouldSendSignUpConfirmationEmailSuccessfully() throws MessagingException {
    final var htmlContent = "<html><body>Reset Code: 123456</body></html>";
    final var mimeMessage = mock(MimeMessage.class);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(any(String.class), any(Context.class))).thenReturn(htmlContent);

    emailService.sendSignUpConfirmation("test@conectabyte.com.br", "CODE");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);
    verify(templateEngine, times(1)).process(any(String.class), any(Context.class));
  }

  @Test
  void shouldLogErrorSendSignUpConfirmationWhenMessagingExceptionIsThrown() throws Exception {
    final var mimeMessage = mock(MimeMessage.class);

    doAnswer(invocation -> {
      throw new MessagingException("Simulated MessagingException");
    }).when(javaMailSender).createMimeMessage();

    emailService.sendSignUpConfirmation("test@conectabyte.com.br", "CODE");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(0)).send(mimeMessage);
    verify(templateEngine, times(0)).process(any(String.class), any(Context.class));
  }

  @Test
  void shouldSendContactConfirmationEmailSuccessfully() throws MessagingException {
    final var htmlContent = "<html><body>We received your contact request. Please use the code below to confirm your e-mail address: 123456</body></html>";
    final var mimeMessage = mock(MimeMessage.class);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(any(String.class), any(Context.class))).thenReturn(htmlContent);

    emailService.sendContactConfirmation("test@conectabyte.com.br", "123456");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);
    verify(templateEngine, times(1)).process(any(String.class), any(Context.class));
  }

  @Test
  void shouldShowErrorWhenMessagingExceptionIsThrown() throws Exception {
    final var mimeMessage = mock(MimeMessage.class);

    doAnswer(invocation -> {
      throw new MessagingException("Simulated MessagingException");
    }).when(javaMailSender).createMimeMessage();

    emailService.sendContactConfirmation("test@conectabyte.com.br", "CODE");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(0)).send(mimeMessage);
    verify(templateEngine, times(0)).process(any(String.class), any(Context.class));
  }

  @Test
  void shouldSendRequestedServiceCancellationNotificationSuccessfully() throws MessagingException {
    final var htmlContent = "<html><body>Reset Code: 123456</body></html>";
    final var mimeMessage = mock(MimeMessage.class);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(any(String.class), any(Context.class))).thenReturn(htmlContent);

    emailService.sendRequestedServiceCancellationNotification("test@conectabyte.com.br", "CODE");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);
    verify(templateEngine, times(1)).process(any(String.class), any(Context.class));
  }

  @Test
  void shouldLogErrorSendRequestedServiceCancellationNotificationWhenMessagingExceptionIsThrown() throws Exception {
    final var mimeMessage = mock(MimeMessage.class);

    doAnswer(invocation -> {
      throw new MessagingException("Simulated MessagingException");
    }).when(javaMailSender).createMimeMessage();

    emailService.sendRequestedServiceCancellationNotification("test@conectabyte.com.br", "CODE");

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(0)).send(mimeMessage);
    verify(templateEngine, times(0)).process(any(String.class), any(Context.class));
  }
}
