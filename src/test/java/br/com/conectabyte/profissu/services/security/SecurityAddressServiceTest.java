package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import br.com.conectabyte.profissu.services.AddressService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityAddressService Tests")
public class SecurityAddressServiceTest {
  @Mock
  private AddressService addressService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityAddressService securityAddressService;

  @Test
  @DisplayName("Should return true when authenticated user is owner of address")
  void shouldReturnTrueWhenUserIsOwnerOfAddress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    when(addressService.findById(any())).thenReturn(address);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityAddressService.ownershipCheck(user.getId());

    assertTrue(isOwner);
  }

  @Test
  @DisplayName("Should return false when authenticated user is not owner of address")
  void shouldReturnFalseWhenUserIsNotOwnerOfAddress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    when(addressService.findById(any())).thenReturn(address);
    when(securityService.isOwner(any())).thenReturn(false);

    boolean isOwner = securityAddressService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  @DisplayName("Should return false when address is not found")
  void shouldReturnFalseWhenAddressNotFound() {
    when(addressService.findById(any())).thenThrow(new ResourceNotFoundException("Address not found"));

    final var isOwner = securityAddressService.ownershipCheck(any());

    assertFalse(isOwner);
    verify(addressService, times(1)).findById(any());
    verify(securityService, never()).isOwner(any());
  }

  @Test
  @DisplayName("Should return false when an unexpected exception occurs during ownership check")
  void shouldReturnFalseWhenUnexpectedExceptionOccurs() {
    when(addressService.findById(any())).thenThrow(new ResourceNotFoundException("Address not found"));

    final var isOwner = securityAddressService.ownershipCheck(any());

    assertFalse(isOwner);
    verify(addressService, times(1)).findById(any());
    verify(securityService, never()).isOwner(any());
  }
}
