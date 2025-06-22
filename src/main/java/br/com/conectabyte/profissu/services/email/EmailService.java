package br.com.conectabyte.profissu.services.email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import br.com.conectabyte.profissu.dtos.request.SendEmailDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Async
public abstract class EmailService<T> {
  protected final JavaMailSender javaMailSender;
  protected final TemplateEngine templateEngine;
  protected final ProfissuProperties profissuProperties;

  protected final String LOGO_PATH = "/images/profissu.jpeg";

  public abstract void send(T data);

  protected void sendEmail(SendEmailDto sendEmailDto) throws MessagingException {
    log.debug("Preparing to send email to: {} with subject: {}", sendEmailDto.email(), sendEmailDto.subject());

    final var message = javaMailSender.createMimeMessage();
    final var helper = new MimeMessageHelper(message, true);
    final var context = new Context();

    sendEmailDto.variables().forEach(context::setVariable);

    final var htmlContent = templateEngine.process(sendEmailDto.templateName(), context);

    helper.setTo(sendEmailDto.email());
    helper.setSubject(sendEmailDto.subject());
    helper.setText(htmlContent, true);

    javaMailSender.send(message);
    log.info("Email successfully sent to: {} with subject: {}", sendEmailDto.email(), sendEmailDto.subject());
  }
}
