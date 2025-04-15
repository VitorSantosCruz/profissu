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
import br.com.conectabyte.profissu.services.security.SecurityAddressService;
import br.com.conectabyte.profissu.services.security.SecurityContactService;
import br.com.conectabyte.profissu.services.security.SecurityRequestedServiceService;
import br.com.conectabyte.profissu.services.security.SecurityService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
  @Mock
  private ContactService contactService;

  @Mock
  private AddressService addressService;

  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private JwtService jwtService;

  @Mock
  private SecurityService mockedSecurityService;

  @InjectMocks
  private SecurityService securityService;

  @InjectMocks
  private SecurityContactService securityContactService;

  @InjectMocks
  private SecurityAddressService securityAddressService;

  @InjectMocks
  private SecurityRequestedServiceService securityRequestedServiceService;

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

    user.setId(0L);
    when(contactService.findById(any())).thenReturn(contact);
    when(mockedSecurityService.isOwner(any())).thenReturn(true);

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

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfAddress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    user.setId(0L);
    when(addressService.findById(any())).thenReturn(address);
    when(mockedSecurityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityAddressService.ownershipCheck(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfAddress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    when(addressService.findById(any())).thenReturn(address);

    final var isOwner = securityAddressService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenAddressNotFound() {
    when(addressService.findById(any())).thenThrow(new ResourceNotFoundException("Address not found"));

    final var isOwner = securityAddressService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    user.setId(0L);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(mockedSecurityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityRequestedServiceService.ownershipCheck(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    when(requestedServiceService.findById(any())).thenReturn(requestedService);

    final var isOwner = securityRequestedServiceService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenRequestedServiceNotFound() {
    when(requestedServiceService.findById(any()))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    final var isOwner = securityRequestedServiceService.ownershipCheck(any());

    assertFalse(isOwner);
  }
}
