package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import br.com.conectabyte.profissu.services.LoginService;

// @WebMvcTest(LoginController.class)
// @ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoginControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private LoginService loginService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    when(loginService.login(any())).thenReturn(new LoginResponseDto(token, expiresIn));

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value(token))
        .andExpect(jsonPath("$.expiresIn").value(expiresIn));
  }

  @Test
  void shouldReturnUnauthorizedWhenBadCredentials() throws Exception {
    final var email = "invalid@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    when(loginService.login(any())).thenThrow(BadCredentialsException.class);

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("bad.credentials.exception"));
  }

  @Test
  void shouldReturnUnauthorizedWhenEmailUnverified() throws Exception {
    final var email = "invalid@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    when(loginService.login(any())).thenThrow(EmailNotVerifiedException.class);

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("email.is.not.verified"));
  }

  @Test
  void shouldReturnBadRequestWhenContentBodyIsInvalid() throws Exception {
    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("bad.request.exception"));
  }

  @Test
  void shouldReturnBadRequestWhenContentEmailIsInvalid() throws Exception {
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(null, password))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("bad.request.exception"));
  }

  @Test
  void shouldReturnBadRequestWhenContentPasswordIsInvalid() throws Exception {
    final var email = "invalid@conectabyte.com.br";

    mockMvc.perform(post("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("bad.request.exception"));
  }
}
