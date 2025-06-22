package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
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
import br.com.conectabyte.profissu.dtos.request.ContactRequestDto;
import br.com.conectabyte.profissu.dtos.response.ContactResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.ContactMapper;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.ContactService;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.services.security.SecurityContactService;
import br.com.conectabyte.profissu.services.security.SecurityService;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ ContactController.class, SecurityService.class, SecurityContactService.class, ProfissuProperties.class })
@Import(SecurityConfig.class)
@DisplayName("ContactController Tests")
class ContactControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ContactService contactService;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private SecurityService securityService;

  @MockitoBean
  private SecurityContactService securityContactService;

  @Autowired
  private ObjectMapper objectMapper;

  private final ContactMapper contactMapper = ContactMapper.INSTANCE;
  private final Contact contact = ContactUtils.create(UserUtils.create());
  private final ContactRequestDto validRequest = contactMapper.contactToContactRequestDto(contact);
  private final ContactResponseDto responseDto = contactMapper.contactToContactResponseDto(contact);

  @Test
  @WithMockUser
  @DisplayName("Should register contact when user is authenticated")
  void shouldRegisterContactWhenUserIsAuthenticated() throws Exception {
    when(contactService.register(any())).thenReturn(responseDto);
    when(securityService.isOwner(any())).thenReturn(true);
    when(userService.findByEmail(any())).thenThrow(ResourceNotFoundException.class);

    mockMvc.perform(post("/contacts")
        .param("userId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.value").value("test@conectabyte.com.br"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when register contact request is invalid")
  void shouldReturnBadRequestWhenRegisterContactRequestIsInvalid() throws Exception {
    final var invalidRequest = new ContactRequestDto("invalidEmail", false);
    when(securityService.isOwner(any())).thenReturn(true);

    mockMvc.perform(post("/contacts")
        .param("userId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @DisplayName("Should return unauthorized when registering contact and user is not authenticated")
  void shouldReturnUnauthorizedOnRegisterWhenUserIsNotAuthenticated() throws Exception {
    mockMvc.perform(post("/contacts")
        .param("userId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should update contact when user is owner")
  void shouldUpdateContactWhenUserIsOwner() throws Exception {
    when(contactService.update(any(), any())).thenReturn(responseDto);
    when(securityContactService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(put("/contacts/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("test@conectabyte.com.br"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when contact does not exist")
  void shouldReturnNotFoundWhenContactDoesNotExist() throws Exception {
    when(contactService.update(any(), any())).thenThrow(new ResourceNotFoundException("Contact not found"));
    when(securityContactService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(put("/contacts/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Contact not found"));
  }

  @Test
  @DisplayName("Should return unauthorized when updating contact and user is not authenticated")
  void shouldReturnUnauthorizedOnUpdateWhenUserIsNotAuthenticated() throws Exception {
    mockMvc.perform(put("/contacts/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on registration")
  void shouldReturnBadRequestForMalformedJsonOnRegister() throws Exception {
    mockMvc.perform(post("/contacts")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on update")
  void shouldReturnBadRequestForMalformedJsonOnUpdate() throws Exception {
    when(securityContactService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(put("/contacts/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }
}
