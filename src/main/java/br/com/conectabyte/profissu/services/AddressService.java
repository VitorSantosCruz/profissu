package br.com.conectabyte.profissu.services;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.AddressRequestDto;
import br.com.conectabyte.profissu.dtos.response.AddressResponseDto;
import br.com.conectabyte.profissu.entities.Address;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.repositories.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {
  private final AddressRepository addressRepository;
  private final UserService userService;

  private final AddressMapper addressMapper = AddressMapper.INSTANCE;

  public AddressResponseDto register(Long userId, AddressRequestDto addressRequestDto) {
    final var addressToBeSaved = addressMapper.addressRequestDtoToAddress(addressRequestDto);
    final var user = this.userService.findById(userId);

    addressToBeSaved.setUser(user);

    final var savedAddress = addressRepository.save(addressToBeSaved);

    return addressMapper.addressToAddressResponseDto(savedAddress);
  }

  public AddressResponseDto update(Long id, AddressRequestDto addressRequestDto) {
    final var address = findById(id);

    address.setStreet(addressRequestDto.street());
    address.setNumber(addressRequestDto.number());
    address.setCity(addressRequestDto.city());
    address.setState(addressRequestDto.state());
    address.setZipCode(addressRequestDto.zipCode());

    final var updatedAddress = addressRepository.save(address);

    return addressMapper.addressToAddressResponseDto(updatedAddress);
  }

  public Address findById(Long id) {
    final var optionalAddress = addressRepository.findById(id);
    final var address = optionalAddress.orElseThrow(() -> new ResourceNotFoundException("Address not found."));

    return address;
  }
}
