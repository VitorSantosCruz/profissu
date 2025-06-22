package br.com.conectabyte.profissu.config.filters;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var correlationId = request.getHeader(CORRELATION_ID_HEADER);

    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
      log.debug("Generated new correlation ID: {}", correlationId);
    } else {
      log.debug("Using existing correlation ID from header: {}", correlationId);
    }

    MDC.put(CORRELATION_ID_HEADER, correlationId);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CORRELATION_ID_HEADER);
      log.trace("Removed correlation ID from MDC.");
    }
  }
}