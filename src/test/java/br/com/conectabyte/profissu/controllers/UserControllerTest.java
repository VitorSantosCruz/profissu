package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import br.com.conectabyte.profissu.config.SecurityConfig;
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
}
