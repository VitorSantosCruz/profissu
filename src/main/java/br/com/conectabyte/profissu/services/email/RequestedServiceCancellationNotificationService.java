package br.com.conectabyte.profissu.services.email;

import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import br.com.conectabyte.profissu.dtos.request.SendEmailDto;
import br.com.conectabyte.profissu.dtos.request.TitleEmailDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.EmailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RequestedServiceCancellationNotificationService extends EmailService<TitleEmailDto> {
  public RequestedServiceCancellationNotificationService(JavaMailSender javaMailSender, TemplateEngine templateEngine,
      ProfissuProperties profissuProperties) {
    super(javaMailSender, templateEngine, profissuProperties);
  }

  @Override
  public void send(TitleEmailDto data) {
    final var variables = Map.of(
        "profissuLogoUrl", profissuProperties.getProfissu().getUrl() + LOGO_PATH,
        "serviceName", data.title());
    final var sendEmailDto = new SendEmailDto(data.email(), "Service Request Cancellation - Profisu",
        "service_request_cancellation_email.html", variables);

    try {
      sendEmail(sendEmailDto);
    } catch (MessagingException e) {
      log.error("Failed to send e-mail to {}: {}", data.email(), e.getMessage());
    }
  }
}
