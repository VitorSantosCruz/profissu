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
import br.com.conectabyte.profissu.services.AddressService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class SecurityAddressServiceTest {
  @Mock
  private AddressService addressService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityAddressService securityAddressService;

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfAddress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);

    when(addressService.findById(any())).thenReturn(address);
    when(securityService.isOwner(any())).thenReturn(true);

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
}
