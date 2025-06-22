package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.ContactService;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityContactService Tests")
public class SecurityContactServiceTest {
  @Mock
  private ContactService contactService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityContactService securityContactService;

  private static final Long TEST_CONTACT_ID = 1L;

  @Test
  @DisplayName("Should return true when authenticated user is owner of contact")
  void shouldReturnTrueWhenUserIsOwnerOfContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    when(contactService.findById(any())).thenReturn(contact);
    when(securityService.isOwner(any())).thenReturn(true);

    boolean isOwner = securityContactService.ownershipCheck(TEST_CONTACT_ID);

    assertTrue(isOwner);
    verify(contactService, times(1)).findById(any());
    verify(securityService, times(1)).isOwner(any());
  }

  @Test
  @DisplayName("Should return false when authenticated user is not owner of contact")
  void shouldReturnFalseWhenUserIsNotOwnerOfContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    when(contactService.findById(any())).thenReturn(contact);
    when(securityService.isOwner(any())).thenReturn(false);

    boolean isOwner = securityContactService.ownershipCheck(TEST_CONTACT_ID);

    assertFalse(isOwner);
    verify(contactService, times(1)).findById(any());
    verify(securityService, times(1)).isOwner(any());
  }

  @Test
  @DisplayName("Should return false when contact is not found")
  void shouldReturnFalseWhenContactNotFound() {
    when(contactService.findById(eq(TEST_CONTACT_ID))).thenThrow(new ResourceNotFoundException("Contact not found"));

    boolean isOwner = securityContactService.ownershipCheck(TEST_CONTACT_ID);

    assertFalse(isOwner);
    verify(contactService, times(1)).findById(eq(TEST_CONTACT_ID));
    verify(securityService, never()).isOwner(any());
  }

  @Test
  @DisplayName("Should return false when an unexpected exception occurs during ownership check")
  void shouldReturnFalseWhenUnexpectedExceptionOccurs() {
    when(contactService.findById(eq(TEST_CONTACT_ID))).thenThrow(new RuntimeException("Simulated unexpected error"));

    boolean isOwner = securityContactService.ownershipCheck(TEST_CONTACT_ID);

    assertFalse(isOwner);
    verify(contactService, times(1)).findById(eq(TEST_CONTACT_ID));
    verify(securityService, never()).isOwner(any());
  }
}
