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
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.mappers.RequestedServiceMapper;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.services.security.SecurityRequestedServiceService;
import br.com.conectabyte.profissu.services.security.SecurityService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ RequestedServiceController.class, SecurityService.class, SecurityRequestedServiceService.class,
    ProfissuProperties.class })
@Import(SecurityConfig.class)
@DisplayName("RequestedServiceController Tests")
class RequestedServiceControllerTest {
  @MockitoBean
  private RequestedServiceService requestedServiceService;

  @MockitoBean
  private SecurityService securityService;

  @MockitoBean
  private SecurityRequestedServiceService securityRequestedServiceService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser
  @DisplayName("Should find available service requests successfully")
  void shouldFindAvailableServiceRequestsSuccessfully() throws Exception {
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
  @DisplayName("Should return unauthorized when finding available service requests and user is not authenticated")
  void shouldReturnUnauthorizedOnFindAvailableServiceRequests() throws Exception {
    mockMvc.perform(get("/requested-services")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should find requested services by user ID successfully")
  void shouldFindRequestedServicesByUserIdSuccessfully() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());
    final var requestedServiceResponseDto = RequestedServiceMapper.INSTANCE
        .requestedServiceToRequestedServiceResponseDto(requestedService);
    final var page = new PageImpl<>(List.of(requestedServiceResponseDto));

    when(requestedServiceService.findByUserId(anyLong(), any())).thenReturn(page);

    mockMvc.perform(get("/requested-services/by-user")
        .param("userId", "1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0]").exists());
  }

  @Test
  @DisplayName("Should return unauthorized when finding requested services by user ID and user is not authenticated")
  void shouldReturnUnauthorizedOnFindByUserId() throws Exception {
    mockMvc.perform(get("/requested-services/by-user")
        .param("userId", "1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when user ID does not exist on find by user ID")
  void shouldReturnNotFoundWhenUserDoesNotExistOnFindByUserId() throws Exception {
    when(requestedServiceService.findByUserId(anyLong(), any()))
        .thenThrow(new ResourceNotFoundException("User not found"));

    mockMvc.perform(get("/requested-services/by-user")
        .param("userId", "999")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should register requested service successfully")
  void shouldRegisterRequestedServiceSuccessfully() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressRequestDto = AddressMapper.INSTANCE.addressToAddressRequestDto(address);
    final var addressResponseDto = AddressMapper.INSTANCE.addressToAddressResponseDto(address);
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description", addressRequestDto);
    final var requestedServiceResponseDto = new RequestedServiceResponseDto(1L, "Title", "Description",
        RequestedServiceStatusEnum.PENDING, addressResponseDto, null);

    when(requestedServiceService.register(any(RequestedServiceRequestDto.class)))
        .thenReturn(requestedServiceResponseDto);

    mockMvc.perform(post("/requested-services")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestedServiceRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  @DisplayName("Should return unauthorized when registering requested service and user is not authenticated")
  void shouldReturnUnauthorizedOnRegisterRequestedService() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressRequestDto = AddressMapper.INSTANCE.addressToAddressRequestDto(address);
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description", addressRequestDto);

    mockMvc.perform(post("/requested-services")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestedServiceRequestDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for invalid input on register")
  void shouldReturnBadRequestForInvalidInputOnRegister() throws Exception {
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("", "", null);

    mockMvc.perform(post("/requested-services")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestedServiceRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when address does not exist on register requested service")
  void shouldReturnNotFoundWhenAddressDoesNotExistOnRegisterRequestedService() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressRequestDto = AddressMapper.INSTANCE.addressToAddressRequestDto(address);
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description", addressRequestDto);

    when(requestedServiceService.register(any(RequestedServiceRequestDto.class)))
        .thenThrow(new ResourceNotFoundException("Address not found."));

    mockMvc.perform(post("/requested-services")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestedServiceRequestDto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Address not found."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on register requested service")
  void shouldReturnBadRequestForMalformedJsonOnRegisterRequestedService() throws Exception {
    mockMvc.perform(post("/requested-services")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should cancel requested service successfully")
  void shouldCancelRequestedServiceSuccessfully() throws Exception {
    final long serviceId = 1L;
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressResponseDto = AddressMapper.INSTANCE.addressToAddressResponseDto(address);
    final var userResponseDto = UserMapper.INSTANCE.userToUserResponseDto(user);
    final var requestedServiceResponseDto = new RequestedServiceResponseDto(serviceId, "Title", "Description",
        RequestedServiceStatusEnum.CANCELLED, addressResponseDto, userResponseDto);

    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(true);
    when(requestedServiceService.changeStatusTOcancelOrDone(anyLong(), any(RequestedServiceStatusEnum.class)))
        .thenReturn(requestedServiceResponseDto);

    mockMvc.perform(patch("/requested-services/{id}/CANCELLED", serviceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should mark requested service as done successfully")
  void shouldMarkRequestedServiceAsDoneSuccessfully() throws Exception {
    final long serviceId = 1L;
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressResponseDto = AddressMapper.INSTANCE.addressToAddressResponseDto(address);
    final var userResponseDto = UserMapper.INSTANCE.userToUserResponseDto(user);
    final var requestedServiceResponseDto = new RequestedServiceResponseDto(serviceId, "Title", "Description",
        RequestedServiceStatusEnum.DONE, addressResponseDto, userResponseDto);

    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(true);
    when(requestedServiceService.changeStatusTOcancelOrDone(anyLong(), any(RequestedServiceStatusEnum.class)))
        .thenReturn(requestedServiceResponseDto);

    mockMvc.perform(patch("/requested-services/{id}/DONE", serviceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  @DisplayName("Should return unauthorized when changing status and user is not authenticated")
  void shouldReturnUnauthorizedOnChangeStatus() throws Exception {
    mockMvc.perform(patch("/requested-services/{id}/CANCELLED", 1L))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when user not authorized to change status")
  void shouldReturnForbiddenWhenUserNotAuthorizedToChangeStatus() throws Exception {
    final long serviceId = 1L;
    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(false);

    mockMvc.perform(patch("/requested-services/{id}/CANCELLED", serviceId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when requested service not found on change status")
  void shouldReturnNotFoundWhenRequestedServiceNotFoundOnChangeStatus() throws Exception {
    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(true);
    when(requestedServiceService.changeStatusTOcancelOrDone(anyLong(), any(RequestedServiceStatusEnum.class)))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    mockMvc.perform(patch("/requested-services/{id}/CANCELLED", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Requested service not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when invalid status transition")
  void shouldReturnBadRequestWhenInvalidStatusTransition() throws Exception {
    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(true);
    when(requestedServiceService.changeStatusTOcancelOrDone(anyLong(), any(RequestedServiceStatusEnum.class)))
        .thenThrow(new ValidationException("Invalid status transition"));

    mockMvc.perform(patch("/requested-services/{id}/DONE", 1L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid status transition"));
  }
}
