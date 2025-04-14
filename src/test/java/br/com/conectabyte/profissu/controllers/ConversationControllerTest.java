package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.services.ConversationService;

@WebMvcTest({ ConversationController.class })
@Import(SecurityConfig.class)
class ConversationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ConversationService conversationService;

  @Test
  @WithMockUser
  void shouldRegisterConversationSuccessfully() throws Exception {
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");
    final var conversationResponseDto = new ConversationResponseDto(1L, null, null, null, null);

    when(conversationService.start(any())).thenReturn(conversationResponseDto);

    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(conversationRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
    final var conversationRequestDto = new ConversationRequestDto(null, null);

    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(conversationRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }
}
