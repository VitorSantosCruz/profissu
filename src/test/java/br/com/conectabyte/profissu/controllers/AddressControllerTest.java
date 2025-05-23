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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.AddressRequestDto;
import br.com.conectabyte.profissu.dtos.response.AddressResponseDto;
import br.com.conectabyte.profissu.entities.Address;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.AddressService;
import br.com.conectabyte.profissu.services.security.SecurityAddressService;
import br.com.conectabyte.profissu.services.security.SecurityService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ AddressController.class, SecurityService.class, SecurityAddressService.class, ProfissuProperties.class })
@Import(SecurityConfig.class)
class AddressControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AddressService addressService;

  @MockitoBean
  private SecurityService securityService;

  @MockitoBean
  private SecurityAddressService securityAddressService;

  @Autowired
  private ObjectMapper objectMapper;

  private final AddressMapper addressMapper = AddressMapper.INSTANCE;
  private final Long userId = 1L;
  private final Long addressId = 1L;
  private final Address address = AddressUtils.create(UserUtils.create());
  private final AddressRequestDto validRequest = addressMapper.addressToAddressRequestDto(address);
  private final AddressResponseDto responseDto = addressMapper.addressToAddressResponseDto(address);

  @Test
  @WithMockUser
  void shouldRegisterAddressWhenUserIsOwnerOrAdmin() throws Exception {
    when(addressService.register(any(), any())).thenReturn(responseDto);
    when(securityService.isOwner(any())).thenReturn(true);

    mockMvc.perform(post("/addresses/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.street").value("123 Main St"));
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
    final var invalidRequest = new AddressRequestDto("", "", "", "", "invalid");

    mockMvc.perform(post("/addresses/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    mockMvc.perform(post("/addresses/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenUserHasNoPermission() throws Exception {
    mockMvc.perform(post("/addresses/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  void shouldUpdateAddressWhenUserIsOwnerOrAdmin() throws Exception {
    when(addressService.update(any(), any())).thenReturn(responseDto);
    when(securityAddressService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(put("/addresses/" + addressId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.street").value("123 Main St"));
  }

  @Test
  @WithMockUser
  void shouldReturnNotFoundWhenAddressDoesNotExist() throws Exception {
    when(addressService.update(any(), any())).thenThrow(new ResourceNotFoundException("Address not found"));
    when(securityAddressService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(put("/addresses/" + addressId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Address not found"));
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestForMalformedJson() throws Exception {
    mockMvc.perform(post("/addresses/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }
}
