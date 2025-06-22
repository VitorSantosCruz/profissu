package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
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
@DisplayName("AddressService Tests")
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
  @DisplayName("Should register address successfully")
  void shouldRegisterAddressSuccessfully() {
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(user);
    when(addressRepository.save(any(Address.class))).thenReturn(address);

    AddressResponseDto savedAddress = addressService.register(validRequest);

    assertNotNull(savedAddress);
    assertEquals(address.getId(), savedAddress.id());
    assertEquals(address.getStreet(), savedAddress.street());
    assertEquals(address.getNumber(), savedAddress.number());
    assertEquals(address.getCity(), savedAddress.city());
    assertEquals(address.getState(), savedAddress.state());
    assertEquals(address.getZipCode(), savedAddress.zipCode());
    verify(addressRepository).save(any(Address.class));
  }

  @Test
  @DisplayName("Should throw NoSuchElementException when JWT claims are missing on register")
  void shouldThrowNoSuchElementExceptionWhenJwtClaimsAreMissingOnRegister() {
    when(jwtService.getClaims()).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> addressService.register(validRequest));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user not found on register")
  void shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnRegister() {
    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenThrow(new ResourceNotFoundException("User not found."));

    assertThrows(ResourceNotFoundException.class, () -> addressService.register(validRequest));
  }

  @Test
  @DisplayName("Should update address successfully")
  void shouldUpdateAddressSuccessfully() {
    when(addressRepository.findById(anyLong())).thenReturn(Optional.of(address));
    when(addressRepository.save(any(Address.class))).thenReturn(address);

    AddressRequestDto updatedRequest = new AddressRequestDto(
        "Updated Street",
        "200",
        "Updated City",
        "US",
        "98765-432");

    AddressResponseDto updatedAddress = addressService.update(1L, updatedRequest);

    assertNotNull(updatedAddress);
    assertEquals("Updated Street", updatedAddress.street());
    assertEquals("200", updatedAddress.number());
    assertEquals("Updated City", updatedAddress.city());
    assertEquals("US", updatedAddress.state());
    assertEquals("98765-432", updatedAddress.zipCode());
    verify(addressRepository).save(any(Address.class));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when address not found on update")
  void shouldThrowResourceNotFoundExceptionWhenAddressNotFoundOnUpdate() {
    when(addressRepository.findById(anyLong())).thenReturn(Optional.empty());

    AddressRequestDto updatedRequest = new AddressRequestDto(
        "Updated Street",
        "200",
        "Updated City",
        "US",
        "98765-432");

    assertThrows(ResourceNotFoundException.class, () -> addressService.update(1L, updatedRequest));
  }

  @Test
  @DisplayName("Should find address by ID successfully")
  void shouldFindAddressByIdSuccessfully() {
    when(addressRepository.findById(anyLong())).thenReturn(Optional.of(address));

    Address foundAddress = addressService.findById(1L);

    assertNotNull(foundAddress);
    assertEquals(address.getId(), foundAddress.getId());
    assertEquals(address.getStreet(), foundAddress.getStreet());
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when address not found by ID")
  void shouldThrowResourceNotFoundExceptionWhenAddressNotFoundById() {
    when(addressRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> addressService.findById(1L));
  }
}
