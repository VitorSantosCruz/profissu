package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
  @Mock
  private ContactService contactService;

  @Mock
  private AddressService addressService;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private SecurityService securityService;

  @Test
  void shouldReturnTrueWhenInformetedIDIsSameThenToken() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));

    final var isOwner = securityService.isOwner(1L);

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenTheInformetedIDNNotIsSameThenToken() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "0")));

    final var isOwner = securityService.isOwner(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserHaveAdminRole() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("ROLE", "ADMIN")));

    final var isAdmin = securityService.isAdmin();

    assertTrue(isAdmin);
  }

  @Test
  void shouldReturnFalseWhenUserNotHaveAdminRole() {
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("ROLE", "USER")));

    final var isAdmin = securityService.isAdmin();

    assertFalse(isAdmin);
  }

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    when(contactService.findById(any())).thenReturn(contact);
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", user.getId())));

    boolean isOwner = securityService.isOwnerOfContact(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfContact() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    final var resourceUserId = user.getId() + 1;

    when(contactService.findById(any())).thenReturn(contact);
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", resourceUserId)));

    boolean isOwner = securityService.isOwnerOfContact(resourceUserId);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenContactNotFound() {
    when(contactService.findById(1L)).thenThrow(new ResourceNotFoundException("Contact not found"));

    final var isOwner = securityService.isOwnerOfContact(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfAddress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    when(addressService.findById(any())).thenReturn(address);
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", user.getId())));

    final var isOwner = securityService.isOwnerOfAddress(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfAddress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    when(addressService.findById(1L)).thenReturn(address);
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));

    final var isOwner = securityService.isOwnerOfAddress(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenAddressNotFound() {
    when(addressService.findById(1L)).thenThrow(new ResourceNotFoundException("Address not found"));

    final var isOwner = securityService.isOwnerOfAddress(1L);

    assertFalse(isOwner);
  }
}
