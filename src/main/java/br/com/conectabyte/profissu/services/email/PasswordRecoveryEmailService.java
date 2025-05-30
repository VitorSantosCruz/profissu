package br.com.conectabyte.profissu.services.email;

import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import br.com.conectabyte.profissu.dtos.request.EmailCodeDto;
import br.com.conectabyte.profissu.dtos.request.SendEmailDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.EmailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PasswordRecoveryEmailService extends EmailService<EmailCodeDto> {
  public PasswordRecoveryEmailService(JavaMailSender javaMailSender, TemplateEngine templateEngine,
      ProfissuProperties profissuProperties) {
    super(javaMailSender, templateEngine, profissuProperties);
  }

  @Override
  public void send(EmailCodeDto data) {
    final var variables = Map.of(
        "profissuLogoUrl", super.profissuProperties.getProfissu().getUrl() + super.LOGO_PATH,
        "code", data.code(),
        "emailTitle", "Password Recovery",
        "emailMessage",
        "We received a request to reset your password. Please use the following code to create a new password.",
        "footerMessage", "If you didn't request this, you can safely ignore this email.");
    final var sendEmailDto = new SendEmailDto(data.email(), "Password Recovery - Profisu",
        "code-verification-email.html", variables);

    try {
      sendEmail(sendEmailDto);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", data.email(), e.getMessage());
    }
  }
}
