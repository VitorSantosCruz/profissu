package br.com.conectabyte.profissu.config.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Tests")
class CorrelationIdFilterTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @Captor
  private ArgumentCaptor<String> correlationIdCaptor;

  @InjectMocks
  private CorrelationIdFilter correlationIdFilter;

  private MockedStatic<MDC> mockedMDC;

  @BeforeEach
  void setUp() {
    mockedMDC = mockStatic(MDC.class);
  }

  @AfterEach
  void tearDown() {
    if (mockedMDC != null) {
      mockedMDC.close();
    }
  }

  @Test
  @DisplayName("Should generate a new Correlation ID if header is missing")
  void shouldGenerateNewCorrelationIdIfHeaderIsMissing() throws ServletException, IOException {
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

    correlationIdFilter.doFilterInternal(request, response, filterChain);

    mockedMDC.verify(() -> MDC.put(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), correlationIdCaptor.capture()));
    final var capturedId = correlationIdCaptor.getValue();

    assertThat(capturedId).isNotNull().isNotEmpty();
    assertThat(UUID.fromString(capturedId)).isNotNull();

    verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, capturedId);
    verify(filterChain).doFilter(request, response);
    mockedMDC.verify(() -> MDC.remove(CorrelationIdFilter.CORRELATION_ID_HEADER));
  }

  @Test
  @DisplayName("Should generate a new Correlation ID if header is empty")
  void shouldGenerateNewCorrelationIdIfHeaderIsEmpty() throws ServletException, IOException {
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("");

    correlationIdFilter.doFilterInternal(request, response, filterChain);

    mockedMDC.verify(() -> MDC.put(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), correlationIdCaptor.capture()));
    final var capturedId = correlationIdCaptor.getValue();

    assertThat(capturedId).isNotNull().isNotEmpty();
    assertThat(UUID.fromString(capturedId)).isNotNull();

    verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, capturedId);
    verify(filterChain).doFilter(request, response);
    mockedMDC.verify(() -> MDC.remove(CorrelationIdFilter.CORRELATION_ID_HEADER));
  }

  @Test
  @DisplayName("Should use existing Correlation ID if header is present")
  void shouldUseExistingCorrelationIdIfHeaderIsPresent() throws ServletException, IOException {
    final String existingCorrelationId = "test-correlation-id-123";
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(existingCorrelationId);

    correlationIdFilter.doFilterInternal(request, response, filterChain);

    mockedMDC.verify(() -> MDC.put(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId));
    verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId);
    verify(filterChain).doFilter(request, response);
    mockedMDC.verify(() -> MDC.remove(CorrelationIdFilter.CORRELATION_ID_HEADER));
  }

  @Test
  @DisplayName("Should remove Correlation ID from MDC even if filterChain throws ServletException")
  void shouldRemoveCorrelationIdIfFilterChainThrowsServletException() throws ServletException, IOException {
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);
    doThrow(new ServletException("Simulated ServletException")).when(filterChain).doFilter(request, response);

    assertThrows(ServletException.class, () -> correlationIdFilter.doFilterInternal(request, response, filterChain));

    mockedMDC.verify(() -> MDC.put(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), anyString()));
    verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), anyString());
    verify(filterChain).doFilter(request, response);
    mockedMDC.verify(() -> MDC.remove(CorrelationIdFilter.CORRELATION_ID_HEADER));
  }

  @Test
  @DisplayName("Should remove Correlation ID from MDC even if filterChain throws IOException")
  void shouldRemoveCorrelationIdIfFilterChainThrowsIOException() throws ServletException, IOException {
    final String existingCorrelationId = "existing-id-456";
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(existingCorrelationId);
    doThrow(new IOException("Simulated IOException")).when(filterChain).doFilter(request, response);

    assertThrows(IOException.class, () -> correlationIdFilter.doFilterInternal(request, response, filterChain));

    mockedMDC.verify(() -> MDC.put(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), eq(existingCorrelationId)));
    verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), eq(existingCorrelationId));
    verify(filterChain).doFilter(request, response);
    mockedMDC.verify(() -> MDC.remove(CorrelationIdFilter.CORRELATION_ID_HEADER));
  }
}
