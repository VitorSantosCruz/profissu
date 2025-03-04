package br.com.conectabyte.profissu.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
  private final JavaMailSender javaMailSender;
  private final TemplateEngine templateEngine;

  @Value("${profissu.url}")
  private String profissuUrl;

  private final String LOGO_PATH = "/images/profissu.jpeg";

  record SendEmailDto(String email, String subject, String templateName, Map<String, String> variables) {
  }

  private void sendEmail(SendEmailDto sendEmailDto) throws MessagingException {
    final var message = javaMailSender.createMimeMessage();
    final var helper = new MimeMessageHelper(message, true);
    final var context = new Context();

    sendEmailDto.variables().forEach(context::setVariable);

    final var htmlContent = templateEngine.process(sendEmailDto.templateName(), context);

    helper.setTo(sendEmailDto.email());
    helper.setSubject(sendEmailDto.subject());
    helper.setText(htmlContent, true);

    javaMailSender.send(message);
  }

  public void sendPasswordRecoveryEmail(String email, String code) throws MessagingException {
    final var variables = Map.of(
        "profissuLogoUrl", profissuUrl + LOGO_PATH,
        "code", code,
        "emailTitle", "Password Recovery",
        "emailMessage",
        "We received a request to reset your password. Please use the following code to create a new password.",
        "footerMessage", "If you didn't request this, you can safely ignore this email.");
    final var sendEmailDto = new SendEmailDto(email, "Password Recovery - Profisu", "code-verification-email.html",
        variables);

    sendEmail(sendEmailDto);
  }

  @Async
  public void sendSignUpConfirmation(String email, String code) {
    final var variables = Map.of(
        "profissuLogoUrl", profissuUrl + LOGO_PATH,
        "code", code,
        "emailTitle", "Sign Up Confirmation",
        "emailMessage", "Thank you for signing up for Profisu! Please use the code below to confirm your registration:",
        "footerMessage", "If you did not sign up, you can safely ignore this email.");
    final var sendEmailDto = new SendEmailDto(email, "Sign Up Confirmation - Profisu", "code-verification-email.html",
        variables);

    try {
      sendEmail(sendEmailDto);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", email, e.getMessage());
    }
  }

  public void sendContactConfirmation(
      String email,
      String code) {
    final var variables = Map.of(
        "profissuLogoUrl", profissuUrl + LOGO_PATH,
        "code", code,
        "emailTitle", "Contact Confirmation",
        "emailMessage", "We received your contact request. Please use the code below to confirm your e-mail address:",
        "footerMessage", "If you did not request this confirmation, you can safely ignore this email.");
    final var sendEmailDto = new SendEmailDto(email, "Contact Confirmation - Profisu", "code-verification-email.html",
        variables);

    try {
      sendEmail(sendEmailDto);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", email, e.getMessage());
    }
  }

}
