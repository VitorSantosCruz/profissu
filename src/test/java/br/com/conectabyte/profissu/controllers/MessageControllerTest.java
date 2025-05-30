package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.dtos.response.MessageResponseDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.MessageService;
import br.com.conectabyte.profissu.services.security.SecurityMessageService;
import br.com.conectabyte.profissu.services.security.SecurityService;

@WebMvcTest({ MessageController.class, SecurityService.class, SecurityMessageService.class, ProfissuProperties.class })
@Import(br.com.conectabyte.profissu.config.SecurityConfig.class)
class MessageControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private MessageService messageService;

  @MockitoBean
  private SecurityService securityService;

  @MockitoBean
  private SecurityMessageService securityMessageService;

  @Test
  @WithMockUser
  void shouldSendMessageSuccessfully() throws Exception {
    final var messageResponseDto = new MessageResponseDto(null, "Teste", false, null);
    final var messageRequestDto = new MessageRequestDto("Teste");

    when(messageService.sendMessage(any(), any())).thenReturn(messageResponseDto);
    when(securityMessageService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(post("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(messageRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Teste"));
  }

  @Test
  @WithMockUser
  void shouldListMessagesSuccessfully() throws Exception {
    final var message1 = new MessageResponseDto(1L, "Teste 1", false, null);
    final var message2 = new MessageResponseDto(2L, "Teste 2", false, null);
    final var messages = new PageImpl<>(List.of(message1, message2));

    when(messageService.listMessages(any(), any())).thenReturn(messages);
    when(securityMessageService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(get("/messages")
        .param("page", "0")
        .param("size", "10")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].message").value("Teste 1"))
        .andExpect(jsonPath("$.content[1].message").value("Teste 2"));
  }

  @Test
  @WithMockUser
  void shouldMarkMessageAsReadWhenUserIsAdmin() throws Exception {
    when(securityService.isAdmin()).thenReturn(true);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldMarkMessageAsReadWhenUserIsServiceProvider() throws Exception {
    when(securityMessageService.ownershipCheck(any())).thenReturn(true);
    when(securityMessageService.messageReceiver(any())).thenReturn(true);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldMarkMessageAsReadWhenUserIsServiceRequester() throws Exception {
    when(securityMessageService.requestedServiceOwner(any())).thenReturn(true);
    when(securityMessageService.messageReceiver(any())).thenReturn(true);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenUserIsMessageOwner() throws Exception {
    when(securityMessageService.requestedServiceOwner(any())).thenReturn(true);
    when(securityMessageService.messageReceiver(any())).thenReturn(false);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenUserHasNoPermission() throws Exception {
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }
}
