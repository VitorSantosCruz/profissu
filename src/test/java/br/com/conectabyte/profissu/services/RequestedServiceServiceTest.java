package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.RequestedServiceCancellationException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.AddressMapper;
import br.com.conectabyte.profissu.mappers.RequestedServiceMapper;
import br.com.conectabyte.profissu.repositories.RequestedServiceRepository;
import br.com.conectabyte.profissu.services.email.RequestedServiceCancellationNotificationService;
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
  private RequestedServiceCancellationNotificationService requestedServiceCancellationNotificationService;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private RequestedServiceService requestedServiceService;

  @Test
  void shouldFindAvailableServiceRequestsWhenSuccessfully() {
    final var pageable = PageRequest.of(0, 10);
    final var requestedService = new RequestedService();
    final var user = UserUtils.create();

    requestedService.setUser(user);

    final var requestedServicePage = new PageImpl<>(List.of(requestedService));

    when(requestedServiceRepository.findAvailableServiceRequests(pageable)).thenReturn(requestedServicePage);

    Page<RequestedServiceResponseDto> result = requestedServiceService.findAvailableServiceRequests(pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void shouldRegisterRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var addressRequestDto = AddressMapper.INSTANCE.addressToAddressRequestDto(address);
    final var requestedServiceRequestDto = new RequestedServiceRequestDto("Title", "Description", addressRequestDto);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());

    requestedService.setUser(user);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceRepository.save(any())).thenReturn(requestedService);

    final var result = requestedServiceService.register(requestedServiceRequestDto);

    assertNotNull(result);
    assertEquals("Title", result.title());
  }

  @Test
  void shouldThrowExceptionWhenUserNotFound() {
    final var requestDto = new RequestedServiceRequestDto("Title", "Description", null);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenThrow(new ResourceNotFoundException("User not found."));

    Exception exception = assertThrows(ResourceNotFoundException.class,
        () -> requestedServiceService.register(requestDto));
    assertEquals("User not found.", exception.getMessage());
  }

  @Test
  void shouldReturnEmptyPageWhenNoResultsFound() {
    final var pageable = PageRequest.of(0, 10);
    final Page<RequestedService> emptyPage = Page.empty();

    when(requestedServiceRepository.findAvailableServiceRequests(pageable)).thenReturn(emptyPage);

    final var result = requestedServiceService.findAvailableServiceRequests(pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
  }

  @Test
  void shouldCancelRequestedServiceSuccessfully() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    final var rejectedConversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    final var canceledConversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    rejectedConversation.setOfferStatus(OfferStatusEnum.REJECTED);
    canceledConversation.setOfferStatus(OfferStatusEnum.CANCELLED);
    serviceProvider.setContacts(List.of(contact));
    requestedService.setConversations(List.of(conversation, rejectedConversation, canceledConversation));

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));
    when(requestedServiceRepository.save(any())).thenReturn(requestedService);

    final var result = requestedServiceService.changeStatusTOcancelOrDone(requestedService.getId(),
        RequestedServiceStatusEnum.CANCELLED);

    assertNotNull(result);
    assertEquals(RequestedServiceStatusEnum.CANCELLED, result.status());

    verify(requestedServiceRepository).save(any());
    verify(requestedServiceCancellationNotificationService, times(1))
        .send(any());
  }

  @Test
  void shouldDoneRequestedServiceSuccessfully() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    serviceProvider.setContacts(List.of(contact));
    requestedService.setStatus(RequestedServiceStatusEnum.INPROGRESS);
    requestedService.setConversations(List.of(conversation));

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));
    when(requestedServiceRepository.save(any())).thenReturn(requestedService);

    final var result = requestedServiceService.changeStatusTOcancelOrDone(requestedService.getId(),
        RequestedServiceStatusEnum.DONE);

    assertNotNull(result);
    assertEquals(RequestedServiceStatusEnum.DONE, result.status());

    verify(requestedServiceRepository).save(any());
    verify(requestedServiceCancellationNotificationService, times(0)).send(any());
  }

  @Test
  void shouldThrowExceptionWhenServiceCannotBeCancelled() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());

    requestedService.setStatus(RequestedServiceStatusEnum.DONE);

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));

    final var exception = assertThrows(RequestedServiceCancellationException.class,
        () -> requestedServiceService.changeStatusTOcancelOrDone(requestedService.getId(),
            RequestedServiceStatusEnum.CANCELLED));

    assertEquals("Requested service can't be cancelled/done", exception.getMessage());

    verify(requestedServiceRepository, never()).save(any());
  }

  @Test
  void shouldThrowExceptionWhenRequestedServiceNotFound() {
    when(requestedServiceRepository.findById(any())).thenReturn(Optional.empty());

    final var exception = assertThrows(ResourceNotFoundException.class,
        () -> requestedServiceService.changeStatusTOcancelOrDone(0L, RequestedServiceStatusEnum.CANCELLED));

    assertEquals("Requested service not found.", exception.getMessage());
    verify(requestedServiceRepository, never()).save(any());
  }

  @Test
  void shouldFindRequestedServiceByUserIdSuccessfully() {
    final var pageable = PageRequest.of(0, 10);
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());
    final var requestedServiceResponseDto = RequestedServiceMapper.INSTANCE
        .requestedServiceToRequestedServiceResponseDto(requestedService);
    final var requestedServicePage = new PageImpl<>(List.of(requestedService), pageable, 1);
    final var expectedResponsePage = new PageImpl<>(List.of(requestedServiceResponseDto), pageable, 1);

    when(requestedServiceRepository.findByUserId(any(), any())).thenReturn(requestedServicePage);

    final var result = requestedServiceService.findByUserId(1L, pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(expectedResponsePage, result);
  }

  @Test
  void shouldReturnEmptyPageWhenNoRequestedServiceForUserFound() {
    final var pageable = PageRequest.of(0, 10);
    final Page<RequestedService> emptyPage = Page.empty(pageable);
    final var expectedEmptyResponsePage = Page.empty(pageable);

    when(requestedServiceRepository.findByUserId(0L, pageable)).thenReturn(emptyPage);

    final var result = requestedServiceService.findByUserId(0L, pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    assertEquals(expectedEmptyResponsePage, result);
  }

  @Test
  void shouldThrowExceptionWhenTryingToChangeStatusToInProgress() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());

    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));

    final var exception = assertThrows(RequestedServiceCancellationException.class,
        () -> requestedServiceService.changeStatusTOcancelOrDone(requestedService.getId(),
            RequestedServiceStatusEnum.INPROGRESS));

    assertEquals("Status is not valid", exception.getMessage());

    verify(requestedServiceRepository, never()).save(any());
  }

  @Test
  void shouldThrowExceptionWhenTryingToChangeStatusToDone() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());

    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));

    final var exception = assertThrows(RequestedServiceCancellationException.class,
        () -> requestedServiceService.changeStatusTOcancelOrDone(requestedService.getId(),
            RequestedServiceStatusEnum.DONE));

    assertEquals("Requested service can't be cancelled/done", exception.getMessage());

    verify(requestedServiceRepository, never()).save(any());
  }
}
