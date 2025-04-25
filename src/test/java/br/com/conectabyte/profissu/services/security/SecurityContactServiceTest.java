package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
public class SecurityContactServiceTest {
  @Mock
  private ContactService contactService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityContactService securityContactService;

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setId(0L);
    when(contactService.findById(any())).thenReturn(contact);
    when(securityService.isOwner(any())).thenReturn(true);

    boolean isOwner = securityContactService.ownershipCheck(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    user.setId(0L);

    final var resourceUserId = user.getId() + 1;

    when(contactService.findById(any())).thenReturn(contact);

    boolean isOwner = securityContactService.ownershipCheck(resourceUserId);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenContactNotFound() {
    when(contactService.findById(any())).thenThrow(new ResourceNotFoundException("Contact not found"));

    final var isOwner = securityContactService.ownershipCheck(any());

    assertFalse(isOwner);
  }
}
