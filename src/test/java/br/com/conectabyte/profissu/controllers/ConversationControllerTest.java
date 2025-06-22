package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import br.com.conectabyte.profissu.dtos.request.ConversationRequestDto;
import br.com.conectabyte.profissu.dtos.response.ConversationResponseDto;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
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
@DisplayName("ConversationController Tests")
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
  @DisplayName("Should find conversations by current user ID")
  void shouldFindConversationsByUserId() throws Exception {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());
    final var conversationResponseDto = ConversationMapper.INSTANCE.conversationToConversationResponseDto(conversation);
    final var page = new PageImpl<>(List.of(conversationResponseDto));

    when(conversationService.findCurrentUserConversations(any())).thenReturn(page);

    mockMvc.perform(get("/conversations")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0]").exists());
  }

  @Test
  @DisplayName("Should return unauthorized when finding conversations and user is not authenticated")
  void shouldReturnUnauthorizedOnFindConversations() throws Exception {
    mockMvc.perform(get("/conversations")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should register conversation successfully")
  void shouldRegisterConversationSuccessfully() throws Exception {
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello, I'm interested!");
    final var conversationResponseDto = new ConversationResponseDto(1L, null, null, null, null);

    when(conversationService.start(any(ConversationRequestDto.class))).thenReturn(conversationResponseDto);

    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(conversationRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when conversation request is invalid")
  void shouldReturnBadRequestWhenConversationRequestIsInvalid() throws Exception {
    final var conversationRequestDto = new ConversationRequestDto(null, null);

    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(conversationRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @DisplayName("Should return unauthorized when registering conversation and user is not authenticated")
  void shouldReturnUnauthorizedOnRegisterConversation() throws Exception {
    final var conversationRequestDto = new ConversationRequestDto(1L, "Hello!");
    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(conversationRequestDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when requested service for conversation does not exist")
  void shouldReturnNotFoundWhenServiceNotFoundOnRegisterConversation() throws Exception {
    final var conversationRequestDto = new ConversationRequestDto(999L, "Service not found offer!");
    when(conversationService.start(any(ConversationRequestDto.class)))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(conversationRequestDto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Requested service not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when offer already accepted or exists for the same service request")
  void shouldReturnBadRequestWhenOfferAlreadyExistsOnRegisterConversation() throws Exception {
    final var conversationRequestDto = new ConversationRequestDto(1L, "Duplicate offer!");
    when(conversationService.start(any(ConversationRequestDto.class)))
        .thenThrow(new ValidationException("An offer has already been accepted or exists for this service request"));

    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(conversationRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("An offer has already been accepted or exists for this service request"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should cancel a conversation successfully")
  void shouldCancelAConversation() throws Exception {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.CANCELLED);

    final var conversationResponseDto = ConversationMapper.INSTANCE.conversationToConversationResponseDto(conversation);

    when(conversationService.changeOfferStatus(anyLong(), any(OfferStatusEnum.class)))
        .thenReturn(conversationResponseDto);
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offerStatus").value(OfferStatusEnum.CANCELLED.toString()));
  }

  @Test
  @DisplayName("Should return unauthorized when cancelling offer and user is not authenticated")
  void shouldReturnUnauthorizedOnCancelOffer() throws Exception {
    mockMvc.perform(patch("/conversations/1/cancel"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when cancelling offer and user is not owner")
  void shouldReturnForbiddenOnCancelOfferWhenNotOwner() throws Exception {
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(false);

    mockMvc.perform(patch("/conversations/1/cancel"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when conversation does not exist on cancel offer")
  void shouldReturnNotFoundWhenConversationDoesNotExistOnCancelOffer() throws Exception {
    when(conversationService.changeOfferStatus(anyLong(), any(OfferStatusEnum.class)))
        .thenThrow(new ResourceNotFoundException("Conversation not found"));
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(patch("/conversations/999/cancel"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Conversation not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when offer status is invalid for cancellation")
  void shouldReturnBadRequestWhenOfferStatusIsInvalidOnCancelOffer() throws Exception {
    when(conversationService.changeOfferStatus(anyLong(), any(OfferStatusEnum.class)))
        .thenThrow(new ValidationException("Offer cannot be cancelled in its current status"));
    when(securityConversationService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1/cancel"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Offer cannot be cancelled in its current status"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should accept a conversation successfully")
  void shouldAcceptAConversation() throws Exception {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    final var conversationResponseDto = ConversationMapper.INSTANCE.conversationToConversationResponseDto(conversation);

    when(conversationService.changeOfferStatus(anyLong(), any(OfferStatusEnum.class)))
        .thenReturn(conversationResponseDto);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1/ACCEPTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offerStatus").value(OfferStatusEnum.ACCEPTED.toString()));
  }

  @Test
  @WithMockUser
  @DisplayName("Should reject a conversation successfully")
  void shouldRejectAConversation() throws Exception {
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, AddressUtils.create(user), List.of());
    final var conversation = ConversationUtils.create(user, UserUtils.create(), requestedService, List.of());

    conversation.setOfferStatus(OfferStatusEnum.REJECTED);

    final var conversationResponseDto = ConversationMapper.INSTANCE.conversationToConversationResponseDto(conversation);

    when(conversationService.changeOfferStatus(anyLong(), any(OfferStatusEnum.class)))
        .thenReturn(conversationResponseDto);
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1/REJECTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offerStatus").value(OfferStatusEnum.REJECTED.toString()));
  }

  @Test
  @DisplayName("Should return unauthorized when changing offer status and user is not authenticated")
  void shouldReturnUnauthorizedOnChangeOfferStatus() throws Exception {
    mockMvc.perform(patch("/conversations/1/ACCEPTED"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when changing offer status and user is not requested service owner")
  void shouldReturnForbiddenOnChangeOfferStatusWhenNotRequestedServiceOwner() throws Exception {
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(false);

    mockMvc.perform(patch("/conversations/1/ACCEPTED"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when conversation does not exist on change offer status")
  void shouldReturnNotFoundWhenConversationDoesNotExistOnChangeOfferStatus() throws Exception {
    when(conversationService.changeOfferStatus(anyLong(), any(OfferStatusEnum.class)))
        .thenThrow(new ResourceNotFoundException("Conversation not found"));
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(true);

    mockMvc.perform(patch("/conversations/999/ACCEPTED"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Conversation not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when offer status transition is invalid")
  void shouldReturnBadRequestWhenOfferStatusTransitionIsInvalidOnChangeOfferStatus() throws Exception {
    when(conversationService.changeOfferStatus(anyLong(), any(OfferStatusEnum.class)))
        .thenThrow(new ValidationException("Invalid offer status transition"));
    when(securityConversationService.isRequestedServiceOwner(anyLong())).thenReturn(true);

    mockMvc.perform(patch("/conversations/1/ACCEPTED"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid offer status transition"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on starting conversation")
  void shouldReturnBadRequestForMalformedJsonOnStartConversation() throws Exception {
    mockMvc.perform(post("/conversations")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }
}
