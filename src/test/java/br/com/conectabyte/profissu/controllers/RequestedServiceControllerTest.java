package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.services.SecurityService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ RequestedServiceController.class, SecurityService.class })
@Import(SecurityConfig.class)
class RequestedServiceControllerTest {
  @MockitoBean
  private RequestedServiceService requestedServiceService;

  @MockitoBean
  private SecurityService securityService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser
  void shouldFindAvailableServiceRequestsWhenSuccessfully() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressResponseDto = AddressMapper.INSTANCE.addressToAddressResponseDto(address);
    final var userResponseDto = UserMapper.INSTANCE.userToUserResponseDto(user);
    final var expectedPage = new PageImpl<>(List.of(new RequestedServiceResponseDto(1L, "Title",
        "Description", RequestedServiceStatusEnum.PENDING, addressResponseDto, userResponseDto)));

    when(requestedServiceService.findAvailableServiceRequests(any(Pageable.class))).thenReturn(expectedPage);

    mockMvc.perform(get("/requested-services")
        .param("page", "0")
        .param("size", "10")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Title"));
  }

  @Test
  @WithMockUser
  void shouldRegisterRequestedService() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressRequestDto = AddressMapper.INSTANCE.addressToAddressRequestDto(address);
    final var addressResponseDto = AddressMapper.INSTANCE.addressToAddressResponseDto(address);
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description", addressRequestDto);
    final var RequestedServiceResponseDto = new RequestedServiceResponseDto(1L, "Title", "Description",
        RequestedServiceStatusEnum.PENDING, addressResponseDto, null);

    when(securityService.isOwner(any())).thenReturn(true);
    when(requestedServiceService.register(any(), any())).thenReturn(RequestedServiceResponseDto);

    mockMvc.perform(post("/requested-services/{userId}", 0)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestedServiceRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  @WithMockUser
  void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressRequestDto = AddressMapper.INSTANCE.addressToAddressRequestDto(address);
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description", addressRequestDto);

    when(securityService.isOwner(any())).thenReturn(true);
    when(requestedServiceService.register(any(), any())).thenThrow(new ResourceNotFoundException("User not found."));

    mockMvc.perform(post("/requested-services/{userId}", 0)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestedServiceRequestDto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found."));
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestForInvalidInput() throws Exception {
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("", "", null);

    mockMvc.perform(post("/requested-services/{userId}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestedServiceRequestDto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnNotFoundWhenRequestedServiceNotFound() throws Exception {
    when(securityService.isAdmin()).thenReturn(true);
    when(requestedServiceService.cancel(any())).thenThrow(new ResourceNotFoundException("Requested service not found"));

    mockMvc.perform(patch("/requested-services/{id}/cancel", 1L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Requested service not found"));
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenUserNotAuthorizedToCancel() throws Exception {
    final long serviceId = 1L;

    mockMvc.perform(patch("/requested-services/{id}/cancel", serviceId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  void shouldCancelRequestedServiceSuccessfully() throws Exception {
    final long serviceId = 1L;
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressResponseDto = AddressMapper.INSTANCE.addressToAddressResponseDto(address);
    final var userResponseDto = UserMapper.INSTANCE.userToUserResponseDto(user);
    final var requestedServiceResponseDto = new RequestedServiceResponseDto(serviceId, "Title", "Description",
        RequestedServiceStatusEnum.CANCELLED, addressResponseDto, userResponseDto);

    when(securityService.isOwnerOfRequestedService(any())).thenReturn(true);
    when(requestedServiceService.cancel(any())).thenReturn(requestedServiceResponseDto);

    mockMvc.perform(patch("/requested-services/{id}/cancel", serviceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.title").value("Title"));
  }
}
