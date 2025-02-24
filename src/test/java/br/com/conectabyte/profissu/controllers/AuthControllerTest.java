package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.dtos.EmailValueRequestDto;
import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.dtos.LoginResponseDto;
import br.com.conectabyte.profissu.dtos.MessageValueResponseDto;
import br.com.conectabyte.profissu.dtos.ResetPasswordRequestDto;
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

  @MockBean
  private UserService userService;

  @MockBean
  private LoginService loginService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
    final var token = "token_test";
    final var expiresIn = 1L;
    final var email = "test@conectabyte.com.br";
    final var password = "Exceptiony.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    when(loginService.login(any())).thenReturn(new LoginResponseDto(token, expiresIn));

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value(token))
        .andExpect(jsonPath("$.expiresIn").value(expiresIn));
  }

  @Test
  void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
    final var email = "invalid@conectabyte.com.br";
    final var password = "Exceptiony.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var errorMessage = "Credentials is not valid";
    when(loginService.login(any())).thenThrow(new BadCredentialsException(errorMessage));

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(errorMessage));
  }

  @Test
  void shouldReturnUnauthorizedWhenEmailIsNotVerified() throws Exception {
    final var email = "invalid@conectabyte.com.br";
    final var password = "Exceptiony.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";
    final var errorMessage = "E-mail is not verified";
    when(loginService.login(any())).thenThrow(new EmailNotVerifiedException(errorMessage));

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, password))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(errorMessage));
  }

  @Test
  void shouldReturnBadRequestWhenRequestBodyIsMalformed() throws Exception {
    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  void shouldReturnBadRequestWhenEmailIsMissing() throws Exception {
    final var password = "Exceptiony.E2J7CeUXU4G3QUqYJGN.jdo75P7iHVApCRkF.DRmGI8tQy3Tn.G";

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(null, password))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldReturnBadRequestWhenPasswordIsMissing() throws Exception {
    final var email = "invalid@conectabyte.com.br";

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequestDto(email, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldRegisterUserWhenDataIsValid() throws Exception {
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(AddressUtils.create(user)));
    user.setId(1L);
    user.setProfile(new Profile());
    when(userService.register(any(UserRequestDto.class))).thenReturn(userMapper.userToUserResponseDto(user));

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
  void shouldReturnBadRequestWhenEmailIsAlreadyRegistered() throws Exception {
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

  @Test
  void shouldAcceptPasswordRecoveryRequestWhenEmailIsValid() throws Exception {
    final var emailValueRequestDto = new EmailValueRequestDto("test@conectabyte.com.br");
    doNothing().when(userService).recoverPassword(any());

    mockMvc.perform(post("/auth/password-recovery")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(emailValueRequestDto)))
        .andExpect(status().isAccepted());
  }

  @Test
  void shouldReturnBadRequestWhenPasswordRecoveryEmailIsMissing() throws Exception {
    final var emailValueRequestDto = new EmailValueRequestDto(null);

    mockMvc.perform(post("/auth/password-recovery")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(emailValueRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldResetPasswordSuccessfullyWhenDataIsValid() throws Exception {
    final var pesetPasswordRequestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "@Admin123", "CODE");
    when(userService.resetPassword(any()))
        .thenReturn(new MessageValueResponseDto(HttpStatus.OK.value(), "Password was updated."));

    mockMvc.perform(post("/auth/password-reset")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(pesetPasswordRequestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password was updated."));
  }

  @Test
  void shouldReturnBadRequestWhenResetCodeIsInvalid() throws Exception {
    final var pesetPasswordRequestDto = new ResetPasswordRequestDto("invalid@conectabyte.com.br", "@Admin123", "CODE");
    when(userService.resetPassword(any()))
        .thenReturn(new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), "Reset code is invalid."));

    mockMvc.perform(post("/auth/password-reset")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(pesetPasswordRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Reset code is invalid."));
  }

  @Test
  void shouldReturnBadRequestWhenResetEmailIsMissing() throws Exception {
    final var pesetPasswordRequestDto = new ResetPasswordRequestDto(null, "@Admin123", "CODE");

    mockMvc.perform(post("/auth/password-reset")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(pesetPasswordRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldReturnBadRequestWhenResetPasswordIsMissing() throws Exception {
    final var pesetPasswordRequestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", null, "CODE");

    mockMvc.perform(post("/auth/password-reset")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(pesetPasswordRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  void shouldReturnBadRequestWhenResetCodeIsMissing() throws Exception {
    final var pesetPasswordRequestDto = new ResetPasswordRequestDto("test@conectabyte.com.br", "@Admin123", null);

    mockMvc.perform(post("/auth/password-reset")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(pesetPasswordRequestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }
}
