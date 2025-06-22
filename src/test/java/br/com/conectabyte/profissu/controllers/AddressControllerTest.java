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
@DisplayName("AddressController Tests")
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
  private final Address address = AddressUtils.create(UserUtils.create());
  private final AddressRequestDto validRequest = addressMapper.addressToAddressRequestDto(address);
  private final AddressResponseDto responseDto = addressMapper.addressToAddressResponseDto(address);

  @Test
  @WithMockUser
  @DisplayName("Should register address when user is authenticated")
  void shouldRegisterAddressWhenUserIsAuthenticated() throws Exception {
    when(addressService.register(any(AddressRequestDto.class))).thenReturn(responseDto);

    mockMvc.perform(post("/addresses")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.street").value("123 Main St"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when register request is invalid")
  void shouldReturnBadRequestWhenRegisterRequestIsInvalid() throws Exception {
    final var invalidRequest = new AddressRequestDto("", "Suite 101", "12345", "Springfield", "State");

    mockMvc.perform(post("/addresses")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @DisplayName("Should return unauthorized when registering address and user is not authenticated")
  void shouldReturnUnauthorizedOnRegisterWhenUserIsNotAuthenticated() throws Exception {
    mockMvc.perform(post("/addresses")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should update address when user is owner")
  void shouldUpdateAddressWhenUserIsOwner() throws Exception {
    when(addressService.update(anyLong(), any(AddressRequestDto.class))).thenReturn(responseDto);
    when(securityAddressService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(put("/addresses/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.street").value("123 Main St"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when updating address and user is not owner")
  void shouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
    when(securityAddressService.ownershipCheck(anyLong())).thenReturn(false);

    mockMvc.perform(put("/addresses/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when update request is invalid")
  void shouldReturnBadRequestWhenUpdateRequestIsInvalid() throws Exception {
    final var invalidRequest = new AddressRequestDto("", "", "", "", "invalid");
    when(securityAddressService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(put("/addresses/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when address does not exist")
  void shouldReturnNotFoundWhenAddressDoesNotExist() throws Exception {
    when(addressService.update(anyLong(), any(AddressRequestDto.class)))
        .thenThrow(new ResourceNotFoundException("Address not found"));
    when(securityAddressService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(put("/addresses/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Address not found"));
  }

  @Test
  @DisplayName("Should return unauthorized when updating address and user is not authenticated")
  void shouldReturnUnauthorizedOnUpdateWhenUserIsNotAuthenticated() throws Exception {
    mockMvc.perform(put("/addresses/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on registration")
  void shouldReturnBadRequestForMalformedJsonOnRegister() throws Exception {
    mockMvc.perform(post("/addresses")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on update")
  void shouldReturnBadRequestForMalformedJsonOnUpdate() throws Exception {
    when(securityAddressService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(put("/addresses/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }
}
