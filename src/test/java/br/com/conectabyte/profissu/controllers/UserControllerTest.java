package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.ProfileRequestDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.enums.GenderEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.mappers.ContactMapper;
import br.com.conectabyte.profissu.mappers.RequestedServiceMapper;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.services.SecurityService;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ UserController.class, SecurityService.class })
@Import(SecurityConfig.class)
public class UserControllerTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;
  private final ContactMapper contactMapper = ContactMapper.INSTANCE;
  private final AddressMapper addressMapper = AddressMapper.INSTANCE;

  @MockBean
  private UserService userService;

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
  void shouldFindAnUserWhenUserWithIdExists() throws Exception {
    final var user = UserUtils.create();

    when(userService.findByIdAndReturnDto(any())).thenReturn(userMapper.userToUserResponseDto(user));

    mockMvc.perform(get("/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(user.getName()));
  }

  @Test
  @WithMockUser
  void shouldReturnNotFoundWhenUserWithIdNotExists() throws Exception {
    doThrow(new ResourceNotFoundException("User not found.")).when(userService).findByIdAndReturnDto(any());

    mockMvc.perform(get("/users/1"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void shouldAllowProfileDeletionWhenUserIsOwner() throws Exception {
    doNothing().when(userService).deleteById(any());
    when(securityService.isOwner(1L)).thenReturn(true);

    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldAllowProfileDeletionWhenUserIsAdmin() throws Exception {
    doNothing().when(userService).deleteById(any());
    when(securityService.isAdmin()).thenReturn(true);

    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldRejectDeletionRequestWhenUserIsNeitherAdminNorOwner() throws Exception {
    doNothing().when(userService).deleteById(any());

    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser()
  void shouldUpdatePasswordWhenUserIsOwner() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "@newPassword123";

    doNothing().when(userService).updatePassword(any(), any());
    when(securityService.isOwner(1L)).thenReturn(true);

    mockMvc.perform(patch("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void shouldUpdatePasswordWhenUserIsAdmin() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "@newPassword123";

    doNothing().when(userService).updatePassword(any(), any());
    when(securityService.isAdmin()).thenReturn(true);

    mockMvc.perform(patch("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void shouldRejectUpdatePasswordRequestWhenUserIsNeitherAdminNorOwner() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "@newPassword123";

    mockMvc.perform(patch("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void shouldRejectUpdatePasswordRequestWhenCurrentPasswordIsNotValid() throws Exception {
    final var newPassword = "newPassword";

    mockMvc.perform(patch("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(null, newPassword))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldRejectUpdatePasswordRequestWhenNewPasswordIsNotValid() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "newPassword";

    mockMvc.perform(patch("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldUpdateUserProfileWhenUserIsOwnerOrAdmin() throws Exception {
    final var userId = 1L;
    final var user = UserUtils.create();
    final var newName = "New Name";
    final var newBio = "New Bio";
    final var newGender = GenderEnum.FEMALE;
    final var profileRequestDto = new ProfileRequestDto(newName, newBio, newGender);
    final var contacts = List.of(contactMapper.contactToContactResponseDto(ContactUtils.create(user)));
    final var addresses = List.of(addressMapper.addressToAddressResponseDto(AddressUtils.create(user)));
    final var updatedUser = new UserResponseDto(userId, newName, newBio, newGender, contacts, addresses);

    when(userService.update(any(), any())).thenReturn(updatedUser);
    when(securityService.isOwner(any())).thenReturn(true);

    mockMvc.perform(put("/users/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(profileRequestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(newName))
        .andExpect(jsonPath("$.bio").value(newBio))
        .andExpect(jsonPath("$.gender").value(newGender.name()));
  }

  @Test
  @WithMockUser
  void shouldRejectProfileUpdateWhenNameIsInvalid() throws Exception {
    final var userId = 1L;
    final var invalidProfileRequestDto = new ProfileRequestDto("Abc", "Valid bio", GenderEnum.FEMALE);

    mockMvc.perform(put("/users/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidProfileRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  void shouldRejectProfileUpdateWhenNameIsEmpty() throws Exception {
    final var userId = 1L;
    final var invalidProfileRequestDto = new ProfileRequestDto(null, "Valid bio", GenderEnum.FEMALE);

    mockMvc.perform(put("/users/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidProfileRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  void shouldRejectProfileUpdateWhenGenderIsNull() throws Exception {
    final var userId = 1L;
    final var invalidProfileRequestDto = new ProfileRequestDto("Valid Name", "Valid bio", null);

    mockMvc.perform(put("/users/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidProfileRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  void shouldRejectProfileUpdateWhenJsonIsMalformed() throws Exception {
    final var userId = 1L;
    final var malformedJson = "{ \"name\": \"Valid Name\", \"bio\": \"Valid bio\", \"gender\": }";

    mockMvc.perform(put("/users/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(malformedJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  void shouldRejectProfileUpdateWhenUserIsNotOwnerOrAdmin() throws Exception {
    final var userId = 1L;
    final var validProfileRequestDto = new ProfileRequestDto("Valid Name", "Valid bio", GenderEnum.FEMALE);

    when(securityService.isOwner(userId)).thenReturn(false);
    when(securityService.isAdmin()).thenReturn(false);

    mockMvc.perform(put("/users/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validProfileRequestDto)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  void shouldRejectProfileUpdateWhenUserIsNotAuthenticated() throws Exception {
    final var userId = 1L;
    final var validProfileRequestDto = new ProfileRequestDto("Valid Name", "Valid bio", GenderEnum.FEMALE);

    mockMvc.perform(put("/users/" + userId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validProfileRequestDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void shouldFindRequestedServicesByUserId() throws Exception {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);
    final var requestedServiceResponseDto = RequestedServiceMapper.INSTANCE
        .requestedServiceToRequestedServiceResponseDto(requestedService);
    final var page = new PageImpl<>(List.of(requestedServiceResponseDto));

    when(requestedServiceService.findByUserId(any(), any())).thenReturn(page);

    mockMvc.perform(get("/users/1/requested-services")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0]").exists());
  }
}
