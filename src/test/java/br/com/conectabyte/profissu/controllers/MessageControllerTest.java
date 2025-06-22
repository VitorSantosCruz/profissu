package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
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

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.dtos.response.MessageResponseDto;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.MessageService;
import br.com.conectabyte.profissu.services.security.SecurityConversationService;
import br.com.conectabyte.profissu.services.security.SecurityMessageService;

@WebMvcTest({ MessageController.class, SecurityMessageService.class, SecurityConversationService.class,
    ProfissuProperties.class })
@Import(SecurityConfig.class)
@DisplayName("MessageController Tests")
class MessageControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private MessageService messageService;

  @MockitoBean
  private SecurityMessageService securityMessageService;

  @MockitoBean
  private SecurityConversationService securityConversationService;

  @Test
  @WithMockUser
  @DisplayName("Should list messages successfully when is conversation owner")
  void shouldListMessagesSuccessfullyWhenIsConversationOwner() throws Exception {
    final var message1 = new MessageResponseDto(1L, "Test 1", false, null);
    final var message2 = new MessageResponseDto(2L, "Test 2", false, null);
    final var messages = new PageImpl<>(List.of(message1, message2));

    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(false);
    when(messageService.listMessages(anyLong(), any())).thenReturn(messages);

    mockMvc.perform(get("/messages")
        .param("page", "0")
        .param("size", "10")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].message").value("Test 1"))
        .andExpect(jsonPath("$.content[1].message").value("Test 2"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should list messages successfully when is requested service owner")
  void shouldListMessagesSuccessfullyWhenIsRequestedServiceOwner() throws Exception {
    final var message1 = new MessageResponseDto(1L, "Test 1", false, null);
    final var message2 = new MessageResponseDto(2L, "Test 2", false, null);
    final var messages = new PageImpl<>(List.of(message1, message2));

    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(true);
    when(messageService.listMessages(anyLong(), any())).thenReturn(messages);

    mockMvc.perform(get("/messages")
        .param("page", "0")
        .param("size", "10")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].message").value("Test 1"))
        .andExpect(jsonPath("$.content[1].message").value("Test 2"));
  }

  @Test
  @DisplayName("Should return unauthorized when listing messages and user is not authenticated")
  void shouldReturnUnauthorizedOnListMessages() throws Exception {
    mockMvc.perform(get("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when listing messages and user is neither conversation owner nor requested service owner")
  void shouldReturnForbiddenOnListMessagesWhenNotAuthorized() throws Exception {
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(false);

    mockMvc.perform(get("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when conversation does not exist on list messages")
  void shouldReturnNotFoundWhenConversationDoesNotExistOnListMessages() throws Exception {
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);
    when(messageService.listMessages(anyLong(), any()))
        .thenThrow(new ResourceNotFoundException("Conversation not found"));

    mockMvc.perform(get("/messages")
        .param("conversationId", "999")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Conversation not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should send message successfully when is conversation owner")
  void shouldSendMessageSuccessfullyWhenIsConversationOwner() throws Exception {
    final var messageResponseDto = new MessageResponseDto(null, "Test", false, null);
    final var messageRequestDto = new MessageRequestDto("Test");

    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(false);
    when(messageService.sendMessage(anyLong(), any(MessageRequestDto.class))).thenReturn(messageResponseDto);

    mockMvc.perform(post("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(messageRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Test"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should send message successfully when is requested service owner")
  void shouldSendMessageSuccessfullyWhenIsRequestedServiceOwner() throws Exception {
    final var messageResponseDto = new MessageResponseDto(null, "Test", false, null);
    final var messageRequestDto = new MessageRequestDto("Test");

    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(true);
    when(messageService.sendMessage(anyLong(), any(MessageRequestDto.class))).thenReturn(messageResponseDto);

    mockMvc.perform(post("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(messageRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Test"));
  }

  @Test
  @DisplayName("Should return unauthorized when sending message and user is not authenticated")
  void shouldReturnUnauthorizedOnSendMessage() throws Exception {
    final var messageRequestDto = new MessageRequestDto("Hello!");
    mockMvc.perform(post("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(messageRequestDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when sending message and user is neither conversation owner nor requested service owner")
  void shouldReturnForbiddenOnSendMessageWhenNotAuthorized() throws Exception {
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(false);

    final var messageRequestDto = new MessageRequestDto("Hello!");
    mockMvc.perform(post("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(messageRequestDto)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when conversation does not exist on send message")
  void shouldReturnNotFoundWhenConversationDoesNotExistOnSendMessage() throws Exception {
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);
    when(messageService.sendMessage(anyLong(), any(MessageRequestDto.class)))
        .thenThrow(new ResourceNotFoundException("Conversation not found"));

    final var messageRequestDto = new MessageRequestDto("Hello!");
    mockMvc.perform(post("/messages")
        .param("conversationId", "999")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(messageRequestDto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Conversation not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when message request is invalid")
  void shouldReturnBadRequestWhenMessageRequestIsInvalid() throws Exception {
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);
    final var invalidMessageRequestDto = new MessageRequestDto("");

    mockMvc.perform(post("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidMessageRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on sending message")
  void shouldReturnBadRequestForMalformedJsonOnSendMessage() throws Exception {
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(post("/messages")
        .param("conversationId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should mark message as read successfully when user is message receiver")
  void shouldMarkMessageAsReadSuccessfully() throws Exception {
    when(securityMessageService.isMessageReceiver(anyLong())).thenReturn(true);
    doNothing().when(messageService).markAsRead(anyLong());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isAccepted());
  }

  @Test
  @DisplayName("Should return unauthorized when marking message as read and user is not authenticated")
  void shouldReturnUnauthorizedOnMarkMessageAsRead() throws Exception {
    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when user is not message receiver")
  void shouldReturnForbiddenWhenUserIsNotMessageReceiver() throws Exception {
    when(securityMessageService.isMessageReceiver(anyLong())).thenReturn(false);

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when message does not exist on mark as read")
  void shouldReturnNotFoundWhenMessageDoesNotExistOnMarkAsRead() throws Exception {
    when(securityMessageService.isMessageReceiver(anyLong())).thenReturn(true);
    doThrow(new ResourceNotFoundException("Message not found")).when(messageService).markAsRead(anyLong());

    mockMvc.perform(patch("/messages/999/read"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Message not found"));
  }
}
