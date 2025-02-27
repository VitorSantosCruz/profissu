package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.utils.UserUtils;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
public class UserControllerTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;

  @MockBean
  private UserService userService;

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
}
