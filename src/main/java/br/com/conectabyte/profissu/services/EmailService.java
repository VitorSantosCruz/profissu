package br.com.conectabyte.profissu.services;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.google.common.hash.Hashing;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
  private final JavaMailSender javaMailSender;
  private final TemplateEngine templateEngine;

  @Value("${profissu.url}")
  private String profissuUrl;

  public void sendPasswordRecoveryEmail(String email) throws MessagingException {
    final var token = Hashing.sha256().hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8).toString();
    final var resetLink = profissuUrl + "/reset-password?token=" + token;
    final var message = javaMailSender.createMimeMessage();
    final var helper = new MimeMessageHelper(message, true);
    final var context = new Context();
    final var profissuLogoUrl = profissuUrl + "/images/profissu.jpeg";

    helper.setTo(email);
    helper.setSubject("Password Recovery - Profisu");
    context.setVariable("profissuLogoUrl", profissuLogoUrl);
    context.setVariable("resetLink", resetLink);
    
    final var htmlContent = templateEngine.process("email-recovery", context);
    helper.setText(htmlContent, true);

    javaMailSender.send(message);
  }
}
