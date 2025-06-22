package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.dtos.request.ProfileRequestDto;
import br.com.conectabyte.profissu.dtos.response.UserResponseDto;
import br.com.conectabyte.profissu.enums.GenderEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.mappers.ContactMapper;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.ConversationService;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.services.security.SecurityService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ UserController.class, SecurityService.class, ProfissuProperties.class })
@Import(SecurityConfig.class)
@DisplayName("UserController Tests")
class UserControllerTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;
  private final ContactMapper contactMapper = ContactMapper.INSTANCE;
  private final AddressMapper addressMapper = AddressMapper.INSTANCE;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private RequestedServiceService requestedServiceService;

  @MockitoBean
  private ConversationService conversationService;

  @MockitoBean
  private SecurityService securityService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser
  @DisplayName("Should find a user when user with ID exists")
  void shouldFindAnUserWhenUserWithIdExists() throws Exception {
    final var user = UserUtils.create();

    when(userService.findByIdAndReturnDto(anyLong())).thenReturn(userMapper.userToUserResponseDto(user));

    mockMvc.perform(get("/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(user.getName()));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when user with ID does not exist")
  void shouldReturnNotFoundWhenUserWithIdNotExists() throws Exception {
    doThrow(new ResourceNotFoundException("User not found.")).when(userService).findByIdAndReturnDto(anyLong());

    mockMvc.perform(get("/users/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found."));
  }

  @Test
  @DisplayName("Should return unauthorized when finding user by ID and user is not authenticated")
  void shouldReturnUnauthorizedOnFindById() throws Exception {
    mockMvc.perform(get("/users/1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should allow profile deletion when user is owner")
  void shouldAllowProfileDeletionWhenUserIsOwner() throws Exception {
    doNothing().when(userService).deleteById(anyLong());
    when(securityService.isOwner(anyLong())).thenReturn(true);
    when(securityService.isAdmin()).thenReturn(false);

    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should allow profile deletion when user is admin")
  void shouldAllowProfileDeletionWhenUserIsAdmin() throws Exception {
    doNothing().when(userService).deleteById(anyLong());
    when(securityService.isAdmin()).thenReturn(true);
    when(securityService.isOwner(anyLong())).thenReturn(false);

    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  @DisplayName("Should reject deletion request when user is neither admin nor owner")
  void shouldRejectDeletionRequestWhenUserIsNeitherAdminNorOwner() throws Exception {
    when(securityService.isOwner(anyLong())).thenReturn(false);
    when(securityService.isAdmin()).thenReturn(false);

    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @DisplayName("Should return unauthorized when deleting user and user is not authenticated")
  void shouldReturnUnauthorizedOnDeleteUser() throws Exception {
    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when user does not exist on delete")
  void shouldReturnNotFoundWhenUserDoesNotExistOnDelete() throws Exception {
    when(securityService.isOwner(anyLong())).thenReturn(true);
    doThrow(new ResourceNotFoundException("User not found")).when(userService).deleteById(anyLong());

    mockMvc.perform(delete("/users/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should update password successfully")
  void shouldUpdatePasswordSuccessfully() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "@newPassword123";

    doNothing().when(userService).updatePassword(any(PasswordRequestDto.class));

    mockMvc.perform(patch("/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should return unauthorized when updating password and user is not authenticated")
  void shouldReturnUnauthorizedOnUpdatePassword() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "@newPassword123";

    mockMvc.perform(patch("/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when current password is not valid")
  void shouldReturnBadRequestWhenCurrentPasswordIsNotValid() throws Exception {
    final var newPassword = "@newPassword123";

    mockMvc.perform(patch("/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(null, newPassword))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when new password is not valid")
  void shouldReturnBadRequestWhenNewPasswordIsNotValid() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "newPassword";

    mockMvc.perform(patch("/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when user does not exist on update password")
  void shouldReturnNotFoundWhenUserDoesNotExistOnUpdatePassword() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "@newPassword123";

    doThrow(new ResourceNotFoundException("User not found")).when(userService)
        .updatePassword(any(PasswordRequestDto.class));

    mockMvc.perform(patch("/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on update password")
  void shouldReturnBadRequestForMalformedJsonOnUpdatePassword() throws Exception {
    mockMvc.perform(patch("/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should update user profile successfully")
  void shouldUpdateUserProfileSuccessfully() throws Exception {
    final var userId = 1L;
    final var user = UserUtils.create();
    final var newName = "New Name";
    final var newBio = "New Bio";
    final var newGender = GenderEnum.FEMALE;
    final var profileRequestDto = new ProfileRequestDto(newName, newBio, newGender);
    final var contacts = List.of(contactMapper.contactToContactResponseDto(ContactUtils.create(user)));
    final var addresses = List.of(addressMapper.addressToAddressResponseDto(AddressUtils.create(user)));
    final var updatedUser = new UserResponseDto(userId, newName, newBio, newGender, contacts, addresses);

    when(userService.update(any(ProfileRequestDto.class))).thenReturn(updatedUser);

    mockMvc.perform(put("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(profileRequestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(newName))
        .andExpect(jsonPath("$.bio").value(newBio))
        .andExpect(jsonPath("$.gender").value(newGender.name()));
  }

  @Test
  @DisplayName("Should reject profile update when user is not authenticated")
  void shouldRejectProfileUpdateWhenUserIsNotAuthenticated() throws Exception {
    final var validProfileRequestDto = new ProfileRequestDto("Valid Name", "Valid bio", GenderEnum.FEMALE);

    mockMvc.perform(put("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validProfileRequestDto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when profile update name is invalid")
  void shouldReturnBadRequestWhenProfileUpdateNameIsInvalid() throws Exception {
    final var invalidProfileRequestDto = new ProfileRequestDto("Ab", "Valid bio", GenderEnum.FEMALE);

    mockMvc.perform(put("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidProfileRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when profile update name is null")
  void shouldReturnBadRequestWhenProfileUpdateNameIsNull() throws Exception {
    final var invalidProfileRequestDto = new ProfileRequestDto(null, "Valid bio", GenderEnum.FEMALE);

    mockMvc.perform(put("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidProfileRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when profile update gender is null")
  void shouldReturnBadRequestWhenProfileUpdateGenderIsNull() throws Exception {
    final var invalidProfileRequestDto = new ProfileRequestDto("Valid Name", "Valid bio", null);

    mockMvc.perform(put("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidProfileRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when profile update JSON is malformed")
  void shouldReturnBadRequestWhenProfileUpdateJsonIsMalformed() throws Exception {
    final var malformedJson = "{ \"name\": \"Valid Name\", \"bio\": \"Valid bio\", \"gender\": }";

    mockMvc.perform(put("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(malformedJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when user does not exist on profile update")
  void shouldReturnNotFoundWhenUserDoesNotExistOnProfileUpdate() throws Exception {
    final var profileRequestDto = new ProfileRequestDto("New Name", "New Bio", GenderEnum.FEMALE);

    when(userService.update(any(ProfileRequestDto.class))).thenThrow(new ResourceNotFoundException("User not found"));

    mockMvc.perform(put("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(profileRequestDto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found"));
  }
}
