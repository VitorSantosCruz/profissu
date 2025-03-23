package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.RequestedServiceCancellationException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.repositories.RequestedServiceRepository;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class RequestedServiceServiceTest {
  @Mock
  private RequestedServiceRepository requestedServiceRepository;

  @Mock
  private UserService userService;

  @Mock
  private EmailService emailService;

  @InjectMocks
  private RequestedServiceService requestedServiceService;

  @Test
  void shouldFindByPage() {
    final var pageable = PageRequest.of(0, 10);
    final var requestedService = new RequestedService();
    final var user = UserUtils.create();

    requestedService.setUser(user);

    final var requestedServicePage = new PageImpl<>(List.of(requestedService));

    when(requestedServiceRepository.findAll(pageable)).thenReturn(requestedServicePage);

    Page<RequestedServiceResponseDto> result = requestedServiceService.findByPage(pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void shouldRegisterRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressRequestDto = AddressMapper.INSTANCE.addressToAddressRequestDto(address);
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description", addressRequestDto);
    final var requestedService = RequestedServiceUtils.create(user, address);

    requestedService.setUser(user);

    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceRepository.save(any())).thenReturn(requestedService);

    final var result = requestedServiceService.register(user.getId(), requestedServiceRequestDto);

    assertNotNull(result);
    assertEquals("Title", result.title());
  }

  @Test
  void shouldThrowExceptionWhenUserNotFound() {
    final var userId = 1L;
    final var requestDto = new RequestedServiceRequestDto("Title", "Description", null);

    when(userService.findById(userId)).thenThrow(new ResourceNotFoundException("User not found."));

    Exception exception = assertThrows(ResourceNotFoundException.class,
        () -> requestedServiceService.register(userId, requestDto));
    assertEquals("User not found.", exception.getMessage());
  }

  @Test
  void shouldReturnEmptyPageWhenNoResultsFound() {
    final var pageable = PageRequest.of(0, 10);
    final Page<RequestedService> emptyPage = Page.empty();

    when(requestedServiceRepository.findAll(pageable)).thenReturn(emptyPage);

    final var result = requestedServiceService.findByPage(pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
  }

  @Test
  void shouldCancelRequestedServiceSuccessfully() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    
    serviceProvider.setContacts(List.of(contact));
    requestedService.setConversations(List.of(conversation));

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));
    when(requestedServiceRepository.save(any())).thenReturn(requestedService);

    final var result = requestedServiceService.cancel(requestedService.getId());

    assertNotNull(result);
    assertEquals(RequestedServiceStatusEnum.CANCELLED, result.status());

    verify(requestedServiceRepository).save(any());
    verify(emailService, times(requestedService.getConversations().size()))
        .sendRequestedServiceCancellationNotification(any(), any());
  }

  @Test
  void shouldThrowExceptionWhenServiceCannotBeCancelled() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    requestedService.setStatus(RequestedServiceStatusEnum.DONE);

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));

    final var exception = assertThrows(RequestedServiceCancellationException.class,
        () -> requestedServiceService.cancel(requestedService.getId()));

    assertEquals("Requested service can't be cancelled", exception.getMessage());

    verify(requestedServiceRepository, never()).save(any());
  }

  @Test
  void shouldThrowExceptionWhenRequestedServiceNotFound() {
    when(requestedServiceRepository.findById(any())).thenReturn(Optional.empty());

    final var exception = assertThrows(ResourceNotFoundException.class, () -> requestedServiceService.cancel(0L));

    assertEquals("Requested service not found.", exception.getMessage());
    verify(requestedServiceRepository, never()).save(any());
  }
}
