package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import br.com.conectabyte.profissu.dtos.request.MessageRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.dtos.response.MessageResponseDto;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.mappers.ConversationMapper;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.ConversationService;
import br.com.conectabyte.profissu.services.security.SecurityConversationService;
import br.com.conectabyte.profissu.services.security.SecurityService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ ConversationController.class, SecurityService.class, SecurityConversationService.class,
    ProfissuProperties.class })
@Import(SecurityConfig.class)
class ConversationControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ConversationService conversationService;

  @MockitoBean
  private SecurityService securityService;

  @MockitoBean
  private SecurityConversationService securityConversationService;

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

  @Test
  @WithMockUser
  void shouldCancelAConversation() throws Exception {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());
    final var conversationResponseDto = ConversationMapper.INSTANCE.conversationToConversationResponseDto(conversation);

    when(conversationService.cancel(any())).thenReturn(conversationResponseDto);
    when(securityConversationService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offerStatus").value(OfferStatusEnum.PENDING.toString()));
  }

  @Test
  @WithMockUser
  void shouldAcceptAConversation() throws Exception {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    final var conversationResponseDto = ConversationMapper.INSTANCE.conversationToConversationResponseDto(conversation);

    when(conversationService.acceptOrRejectOffer(any(), any())).thenReturn(conversationResponseDto);
    when(securityConversationService.requestedServiceOwner(any())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1/ACCEPTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offerStatus").value(OfferStatusEnum.ACCEPTED.toString()));
  }

  @Test
  @WithMockUser
  void shouldRejectAConversation() throws Exception {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user));
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.REJECTED);

    final var conversationResponseDto = ConversationMapper.INSTANCE.conversationToConversationResponseDto(conversation);

    when(conversationService.acceptOrRejectOffer(any(), any())).thenReturn(conversationResponseDto);
    when(securityConversationService.requestedServiceOwner(any())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1/REJECTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offerStatus").value(OfferStatusEnum.REJECTED.toString()));
  }

  @Test
  @WithMockUser
  void shouldSendMessageSuccessfully() throws Exception {
    final var messageResponseDto = new MessageResponseDto(null, "Teste", false, null);
    final var messageRequestDto = new MessageRequestDto("Teste");

    when(conversationService.sendMessage(any(), any())).thenReturn(messageResponseDto);
    when(securityConversationService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(post("/conversations/1/messages")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(messageRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Teste"));
  }
}
