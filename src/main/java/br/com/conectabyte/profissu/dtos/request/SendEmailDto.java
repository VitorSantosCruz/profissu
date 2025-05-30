package br.com.conectabyte.profissu.dtos.request;

import java.util.Map;

public record SendEmailDto(String email, String subject, String templateName, Map<String, String> variables) {
}