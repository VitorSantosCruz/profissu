package br.com.conectabyte.profissu.services.email;

import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import br.com.conectabyte.profissu.dtos.request.NotificationEmailDto;
import br.com.conectabyte.profissu.dtos.request.SendEmailDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService extends EmailService<NotificationEmailDto> {
  public NotificationService(JavaMailSender javaMailSender, TemplateEngine templateEngine,
      ProfissuProperties profissuProperties) {
    super(javaMailSender, templateEngine, profissuProperties);
  }

  @Override
  public void send(NotificationEmailDto data) {
    log.info("Attempting to send notification email to: {}", data.email());

    final var variables = Map.of(
        "profissuLogoUrl", profissuProperties.getProfissu().getUrl() + LOGO_PATH,
        "notification", data.notification());
    final var sendEmailDto = new SendEmailDto(data.email(), "Notification - Profisu",
        "notification-email.html",
        variables);

    try {
      sendEmail(sendEmailDto);
    } catch (MessagingException e) {
      log.error("Failed to send notification email to {}: {}", data.email(), e.getMessage());
    }
  }
}
