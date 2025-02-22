package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import br.com.conectabyte.profissu.dtos.UserRequestDto;
import br.com.conectabyte.profissu.entities.Profile;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.EmailNotVerifiedException;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.services.LoginService;
import br.com.conectabyte.profissu.services.UserService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private LoginService loginService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    when(loginService.login(any())).thenReturn(new LoginResponseDto(token, expiresIn));

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value(token))
        .andExpect(jsonPath("$.expiresIn").value(expiresIn));
  }

  @Test
  void shouldReturnUnauthorizedWhenBadCredentials() throws Exception {
    final var email = "invalid@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var errorMessage = "Credentials is not valid";
    when(loginService.login(any())).thenThrow(new BadCredentialsException(errorMessage));

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(errorMessage));
  }

  @Test
  void shouldReturnUnauthorizedWhenEmailUnverified() throws Exception {
    final var email = "invalid@conectabyte.com.br";
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var errorMessage = "E-mail is not verified";
    when(loginService.login(any())).thenThrow(new EmailNotVerifiedException(errorMessage));

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(errorMessage));
  }

  @Test
  void shouldReturnBadRequestWhenContentBodyIsInvalid() throws Exception {
    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  void shouldReturnBadRequestWhenContentEmailIsInvalid() throws Exception {
    final var password = "$2y$10$D.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(null, password))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldReturnBadRequestWhenContentPasswordIsInvalid() throws Exception {
    final var email = "invalid@conectabyte.com.br";

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldReturnSavedUserWhenUserDataIsValid() throws Exception {
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(AddressUtils.create(user)));
    user.setId(1L);
    user.setProfile(new Profile());
    when(userService.save(any(UserRequestDto.class))).thenReturn(userMapper.userToUserResponseDto(user));

    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(userMapper.userToUserRequestDto(user))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.gender").exists())
        .andExpect(jsonPath("$.profile").exists())
        .andExpect(jsonPath("$.contacts").exists())
        .andExpect(jsonPath("$.addresses").exists());
  }

  @Test
  void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(AddressUtils.create(user)));
    when(userService.findByEmail(any())).thenReturn(Optional.of(new User()));

    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(userMapper.userToUserRequestDto(user))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }
}
