package br.com.conectabyte.profissu.services.email;

import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import br.com.conectabyte.profissu.dtos.request.EmailCodeDto;
import br.com.conectabyte.profissu.dtos.request.SendEmailDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SignUpConfirmationService extends EmailService<EmailCodeDto> {
  public SignUpConfirmationService(JavaMailSender javaMailSender, TemplateEngine templateEngine,
      ProfissuProperties profissuProperties) {
    super(javaMailSender, templateEngine, profissuProperties);
  }

  @Override
  public void send(EmailCodeDto data) {
    log.info("Attempting to send sign up confirmation email to: {}", data.email());

    final var variables = Map.of(
        "profissuLogoUrl", profissuProperties.getProfissu().getUrl() + LOGO_PATH,
        "code", data.code(),
        "emailTitle", "Sign Up Confirmation",
        "emailMessage", "Thank you for signing up for Profisu! Please use the code below to confirm your registration:",
        "footerMessage", "If you did not sign up, you can safely ignore this email.");
    final var sendEmailDto = new SendEmailDto(data.email(), "Sign Up Confirmation - Profisu",
        "code-verification-email.html",
        variables);

    try {
      sendEmail(sendEmailDto);
    } catch (MessagingException e) {
      log.error("Failed to send sign up confirmation email to {}: {}", data.email(), e.getMessage());
    }
  }
}
