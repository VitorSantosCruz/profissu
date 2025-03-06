package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.ContactRequestDto;
import br.com.conectabyte.profissu.dtos.response.ContactResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.ContactMapper;
import br.com.conectabyte.profissu.services.ContactService;
import br.com.conectabyte.profissu.services.SecurityService;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ ContactController.class, SecurityService.class })
@Import(SecurityConfig.class)
class ContactControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ContactService contactService;

  @MockBean
  private UserService userService;

  @MockBean
  private SecurityService securityService;

  @Autowired
  private ObjectMapper objectMapper;

  private final ContactMapper contactMapper = ContactMapper.INSTANCE;
  private final Long userId = 1L;
  private final Long contactId = 1L;
  private final Contact contact = ContactUtils.createEmail(UserUtils.create());
  private final ContactRequestDto validRequest = contactMapper.contactToContactRequestDto(contact);
  private final ContactResponseDto responseDto = contactMapper.contactToContactResponseDto(contact);

  @Test
  @WithMockUser
  void shouldRegisterContactWhenUserIsOwnerOrAdmin() throws Exception {
    when(contactService.register(any(), any())).thenReturn(responseDto);
    when(securityService.isOwner(any())).thenReturn(true);
    when(userService.findByEmail(any())).thenThrow(ResourceNotFoundException.class);

    mockMvc.perform(post("/contacts/{userId}", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.value").value("test@conectabyte.com.br"))
        .andExpect(jsonPath("$.type").value("EMAIL"));
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
    final var invalidRequest = new ContactRequestDto(null, "invalidEmail", false);
    when(securityService.isOwner(any())).thenReturn(true);

    mockMvc.perform(post("/contacts/{userId}", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    mockMvc.perform(post("/contacts/{userId}", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenUserHasNoPermission() throws Exception {
    when(securityService.isOwner(any())).thenReturn(false);
    when(securityService.isAdmin()).thenReturn(false);
    when(userService.findByEmail(any())).thenThrow(ResourceNotFoundException.class);

    mockMvc.perform(post("/contacts/{userId}", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  void shouldUpdateContactWhenUserIsOwnerOrAdmin() throws Exception {
    when(contactService.update(any(), any())).thenReturn(responseDto);
    when(securityService.isOwnerOfContact(any())).thenReturn(true);

    mockMvc.perform(put("/contacts/{id}", contactId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("test@conectabyte.com.br"))
        .andExpect(jsonPath("$.type").value("EMAIL"));
  }

  @Test
  @WithMockUser
  void shouldReturnNotFoundWhenContactDoesNotExist() throws Exception {
    when(contactService.update(any(), any())).thenThrow(new ResourceNotFoundException("Contact not found"));
    when(securityService.isOwnerOfContact(any())).thenReturn(true);

    mockMvc.perform(put("/contacts/{id}", contactId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Contact not found"));
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestForMalformedJson() throws Exception {
    mockMvc.perform(post("/contacts/{userId}", userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }
}
