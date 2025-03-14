package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.services.SecurityService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ RequestedServiceController.class, SecurityService.class })
@Import(SecurityConfig.class)
class RequestedServiceControllerTest {
  @MockBean
  private RequestedServiceService requestedServiceService;

  @MockBean
  private SecurityService securityService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser
  void shouldFindByPage() throws Exception {
    Page<RequestedServiceResponseDto> expectedPage = new PageImpl<>(List.of(
        new RequestedServiceResponseDto(1L, "Title", "Description", RequestedServiceStatusEnum.PENDING, null, 1L)));

    when(requestedServiceService.findByPage(any(Pageable.class))).thenReturn(expectedPage);

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
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description",
        addressRequestDto);
    final var RequestedServiceResponseDto = new RequestedServiceResponseDto(1L, "Title",
        "Description",
        RequestedServiceStatusEnum.PENDING, addressResponseDto, user.getId());

    when(securityService.isOwner(any())).thenReturn(true);
    when(requestedServiceService.register(any(), any())).thenReturn(RequestedServiceResponseDto);

    mockMvc.perform(post("/requested-services/{userId}", user.getId())
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
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description",
        addressRequestDto);

    when(securityService.isOwner(any())).thenReturn(true);
    when(requestedServiceService.register(any(), any())).thenThrow(new ResourceNotFoundException("User not found."));

    mockMvc.perform(post("/requested-services/{userId}", user.getId())
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
}
