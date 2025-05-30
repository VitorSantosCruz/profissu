package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.dtos.request.AddressRequestDto;
import br.com.conectabyte.profissu.dtos.response.AddressResponseDto;
import br.com.conectabyte.profissu.entities.Address;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.repositories.AddressRepository;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

  @Mock
  private AddressRepository addressRepository;

  @Mock
  private UserService userService;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private AddressService addressService;

  private AddressMapper addressMapper = AddressMapper.INSTANCE;
  private final User user = UserUtils.create();
  private final Address address = AddressUtils.create(user);
  private final AddressRequestDto validRequest = addressMapper.addressToAddressRequestDto(address);

  @Test
  void shouldRegisterAddressSuccessfully() {
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(1L)).thenReturn(user);
    when(addressRepository.save(any(Address.class))).thenReturn(address);

    AddressResponseDto savedAddress = addressService.register(validRequest);

    assertEquals(savedAddress.id(), address.getId());
    assertEquals(savedAddress.street(), address.getStreet());
    assertEquals(savedAddress.number(), address.getNumber());
    assertEquals(savedAddress.city(), address.getCity());
    assertEquals(savedAddress.state(), address.getState());
    assertEquals(savedAddress.zipCode(), address.getZipCode());
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(1L)).thenThrow(new ResourceNotFoundException("User not found."));

    assertThrows(ResourceNotFoundException.class, () -> addressService.register(validRequest));
  }

  @Test
  void shouldUpdateAddressSuccessfully() {
    when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
    when(addressRepository.save(any(Address.class))).thenReturn(address);

    AddressRequestDto updatedRequest = new AddressRequestDto(
        "Updated Street",
        "200",
        "Updated City",
        "US",
        "98765-432");

    AddressResponseDto updatedAddress = addressService.update(1L, updatedRequest);

    assertEquals("Updated Street", updatedAddress.street());
    assertEquals("200", updatedAddress.number());
    assertEquals("Updated City", updatedAddress.city());
    assertEquals("US", updatedAddress.state());
    assertEquals("98765-432", updatedAddress.zipCode());
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenAddressNotFound() {
    when(addressRepository.findById(1L)).thenReturn(Optional.empty());

    AddressRequestDto updatedRequest = new AddressRequestDto(
        "Updated Street",
        "200",
        "Updated City",
        "US",
        "98765-432");

    assertThrows(ResourceNotFoundException.class, () -> addressService.update(1L, updatedRequest));
  }
}
