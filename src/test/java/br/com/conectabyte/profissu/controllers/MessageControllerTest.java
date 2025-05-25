package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.MessageService;
import br.com.conectabyte.profissu.services.security.SecurityMessageService;
import br.com.conectabyte.profissu.services.security.SecurityService;

@WebMvcTest({ MessageController.class, SecurityService.class, SecurityMessageService.class, ProfissuProperties.class })
@Import(br.com.conectabyte.profissu.config.SecurityConfig.class)
class MessageControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MessageService messageService;

  @MockitoBean
  private SecurityService securityService;

  @MockitoBean
  private SecurityMessageService securityMessageService;

  @Test
  @WithMockUser
  void shouldMarkMessageAsReadWhenUserIsAdmin() throws Exception {
    when(securityService.isAdmin()).thenReturn(true);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldMarkMessageAsReadWhenUserIsServiceProvider() throws Exception {
    when(securityMessageService.ownershipCheck(any())).thenReturn(true);
    when(securityMessageService.messageReciver(any())).thenReturn(true);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldMarkMessageAsReadWhenUserIsServiceRequester() throws Exception {
    when(securityMessageService.requestedServiceOwner(any())).thenReturn(true);
    when(securityMessageService.messageReciver(any())).thenReturn(true);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenUserIsMessageOwner() throws Exception {
    when(securityMessageService.requestedServiceOwner(any())).thenReturn(true);
    when(securityMessageService.messageReciver(any())).thenReturn(false);
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenUserHasNoPermission() throws Exception {
    doNothing().when(messageService).markAsRead(any());

    mockMvc.perform(patch("/messages/1/read"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }
}
