package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import br.com.conectabyte.profissu.dtos.request.PasswordRequestDto;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.services.SecurityService;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest({ UserController.class, SecurityService.class })
@Import(SecurityConfig.class)
public class UserControllerTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;

  @MockBean
  private UserService userService;

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

    when(userService.findById(any())).thenReturn(userMapper.userToUserResponseDto(user));

    mockMvc.perform(get("/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(user.getName()));
  }

  @Test
  @WithMockUser
  void shouldReturnNotFoundWhenUserWithIdNotExists() throws Exception {
    doThrow(new ResourceNotFoundException("User not found.")).when(userService).findById(any());

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

    mockMvc.perform(put("/users/1/password")
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

    mockMvc.perform(put("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void shouldRejectUpdatePasswordRequestWhenUserIsNeitherAdminNorOwner() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "@newPassword123";

    mockMvc.perform(put("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void shouldRejectUpdatePasswordRequestWhenCurrentPasswordIsNotValid() throws Exception {
    final var newPassword = "newPassword";

    mockMvc.perform(put("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(null, newPassword))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldRejectUpdatePasswordRequestWhenNewPasswordIsNotValid() throws Exception {
    final var currentPassword = "currentPassword";
    final var newPassword = "newPassword";

    mockMvc.perform(put("/users/1/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new PasswordRequestDto(currentPassword, newPassword))))
        .andExpect(status().isBadRequest());
  }
}
