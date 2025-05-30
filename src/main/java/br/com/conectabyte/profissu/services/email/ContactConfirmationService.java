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
public class ContactConfirmationService extends EmailService<EmailCodeDto> {
  public ContactConfirmationService(JavaMailSender javaMailSender, TemplateEngine templateEngine,
      ProfissuProperties profissuProperties) {
    super(javaMailSender, templateEngine, profissuProperties);
  }

  @Override
  public void send(EmailCodeDto data) {
    final var variables = Map.of(
        "profissuLogoUrl", profissuProperties.getProfissu().getUrl() + LOGO_PATH,
        "code", data.code(),
        "emailTitle", "Contact Confirmation",
        "emailMessage", "We received your contact request. Please use the code below to confirm your e-mail address:",
        "footerMessage", "If you did not request this confirmation, you can safely ignore this email.");
    final var sendEmailDto = new SendEmailDto(data.email(), "Contact Confirmation - Profisu",
        "code-verification-email.html",
        variables);

    try {
      sendEmail(sendEmailDto);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", data.email(), e.getMessage());
    }
  }
}
