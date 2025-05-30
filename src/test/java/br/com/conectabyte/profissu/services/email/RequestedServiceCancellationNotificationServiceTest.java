package br.com.conectabyte.profissu.services.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import br.com.conectabyte.profissu.dtos.request.TitleEmailDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.utils.PropertiesLoader;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class RequestedServiceCancellationNotificationServiceTest {
  @Mock
  private JavaMailSender javaMailSender;

  @Mock
  private TemplateEngine templateEngine;

  @Mock
  private ProfissuProperties profissuProperties;

  @InjectMocks
  private RequestedServiceCancellationNotificationService requestedServiceCancellationNotificationService;

  @BeforeEach
  void before() throws Exception {
    final var loadedProfissuProperties = new PropertiesLoader().loadProperties();

    when(profissuProperties.getProfissu()).thenReturn(loadedProfissuProperties.getProfissu());
  }

  @Test
  void shouldSendRequestedServiceCancellationNotificationSuccessfully() throws MessagingException {
    final var htmlContent = "<html><body>Reset Code: 123456</body></html>";
    final var mimeMessage = mock(MimeMessage.class);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(any(String.class), any(Context.class))).thenReturn(htmlContent);

    requestedServiceCancellationNotificationService.send(new TitleEmailDto("Title", "test@conectabyte.com.br"));

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

    requestedServiceCancellationNotificationService.send(new TitleEmailDto("Title", "test@conectabyte.com.br"));

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(0)).send(mimeMessage);
    verify(templateEngine, times(0)).process(any(String.class), any(Context.class));
  }
}
