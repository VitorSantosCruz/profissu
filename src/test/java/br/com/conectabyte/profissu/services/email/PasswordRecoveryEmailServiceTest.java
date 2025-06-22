package br.com.conectabyte.profissu.services.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import br.com.conectabyte.profissu.dtos.request.EmailCodeDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.utils.PropertiesLoader;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordRecoveryEmailService Tests")
class PasswordRecoveryEmailServiceTest {

  @Mock
  private JavaMailSender javaMailSender;

  @Mock
  private TemplateEngine templateEngine;

  @Mock
  private ProfissuProperties profissuProperties;

  @InjectMocks
  private PasswordRecoveryEmailService passwordRecoveryEmailService;

  private static final String TEST_EMAIL = "test@conectabyte.com.br";
  private static final String TEST_CODE = "RESETCODE";
  private static final String TEMPLATE_NAME = "code-verification-email.html";

  @BeforeEach
  void before() throws Exception {
    final var loadedProfissuProperties = new PropertiesLoader().loadProperties();

    when(profissuProperties.getProfissu()).thenReturn(loadedProfissuProperties.getProfissu());
  }

  @Test
  @DisplayName("Should send password recovery email successfully")
  void shouldSendPasswordRecoveryEmailSuccessfully() throws MessagingException {
    final var htmlContent = "<html><body>Reset Code: 123456</body></html>";
    final var mimeMessage = mock(MimeMessage.class);
    final var emailCodeDto = new EmailCodeDto(TEST_EMAIL, TEST_CODE);

    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(any(String.class), any(Context.class))).thenReturn(htmlContent);

    passwordRecoveryEmailService.send(emailCodeDto);

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(templateEngine, times(1)).process(eq(TEMPLATE_NAME), any(Context.class));
    verify(javaMailSender, times(1)).send(mimeMessage);
  }

  @Test
  @DisplayName("Should log error and not send email when MessagingException occurs")
  void shouldLogErrorWhenMessagingExceptionIsThrown() throws MessagingException {
    final var emailCodeDto = new EmailCodeDto(TEST_EMAIL, TEST_CODE);

    doAnswer(invocation -> {
      throw new MessagingException("Simulated MessagingException");
    }).when(javaMailSender).createMimeMessage();

    passwordRecoveryEmailService.send(emailCodeDto);

    verify(javaMailSender, times(1)).createMimeMessage();
    verify(templateEngine, times(0)).process(any(String.class), any(Context.class));
    verify(javaMailSender, times(0)).send(any(MimeMessage.class));
  }
}
