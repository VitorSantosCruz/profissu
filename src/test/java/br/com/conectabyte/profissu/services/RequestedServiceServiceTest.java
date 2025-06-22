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

import org.junit.jupiter.api.DisplayName;
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
@DisplayName("RequestedServiceService Tests")
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
  @DisplayName("Should find available service requests successfully")
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
    verify(requestedServiceRepository, times(1)).findAvailableServiceRequests(pageable);
  }

  @Test
  @DisplayName("Should register a new requested service successfully")
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
    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(any());
    verify(requestedServiceRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user not found during registration")
  void shouldThrowExceptionWhenUserNotFound() {
    final var requestDto = new RequestedServiceRequestDto("Title", "Description", null);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenThrow(new ResourceNotFoundException("User not found."));

    Exception exception = assertThrows(ResourceNotFoundException.class,
        () -> requestedServiceService.register(requestDto));
    assertEquals("User not found.", exception.getMessage());
    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(any());
    verify(requestedServiceRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should return empty page when no available services found")
  void shouldReturnEmptyPageWhenNoResultsFound() {
    final var pageable = PageRequest.of(0, 10);
    final Page<RequestedService> emptyPage = Page.empty();

    when(requestedServiceRepository.findAvailableServiceRequests(pageable)).thenReturn(emptyPage);

    final var result = requestedServiceService.findAvailableServiceRequests(pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    verify(requestedServiceRepository, times(1)).findAvailableServiceRequests(pageable);
  }

  @Test
  @DisplayName("Should cancel requested service successfully and notify relevant parties")
  void shouldCancelRequestedServiceSuccessfully() {
    final var user = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    contact.setStandard(true);
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());
    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);
    final var conversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    final var rejectedConversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());
    final var canceledConversation = ConversationUtils.create(user, serviceProvider, requestedService, List.of());

    rejectedConversation.setOfferStatus(OfferStatusEnum.REJECTED);
    canceledConversation.setOfferStatus(OfferStatusEnum.CANCELLED);
    serviceProvider.setContacts(List.of(contact));
    requestedService.setConversations(List.of(conversation, rejectedConversation, canceledConversation));
    conversation.setOfferStatus(OfferStatusEnum.PENDING);

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));
    when(requestedServiceRepository.save(any())).thenReturn(requestedService);

    final var result = requestedServiceService.changeStatusTOcancelOrDone(requestedService.getId(),
        RequestedServiceStatusEnum.CANCELLED);

    assertNotNull(result);
    assertEquals(RequestedServiceStatusEnum.CANCELLED, result.status());
    verify(requestedServiceRepository, times(1)).save(requestedService);
    verify(requestedServiceCancellationNotificationService, times(1)).send(any());
  }

  @Test
  @DisplayName("Should finalize requested service successfully")
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

    verify(requestedServiceRepository, times(1)).save(requestedService);
    verify(requestedServiceCancellationNotificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should throw exception when trying to cancel service that is not PENDING")
  void shouldThrowExceptionWhenTryingToCancelFromNonPendingStatus() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());

    requestedService.setStatus(RequestedServiceStatusEnum.INPROGRESS);

    when(requestedServiceRepository.findById(any())).thenReturn(Optional.of(requestedService));

    final var exception = assertThrows(RequestedServiceCancellationException.class,
        () -> requestedServiceService.changeStatusTOcancelOrDone(requestedService.getId(),
            RequestedServiceStatusEnum.CANCELLED));

    assertEquals("Requested service can't be cancelled/done", exception.getMessage());
    verify(requestedServiceRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when trying to finalize service that is not INPROGRESS")
  void shouldThrowExceptionWhenTryingToDoneFromNonInProgressStatus() {
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

  @Test
  @DisplayName("Should throw ResourceNotFoundException when requested service not found for status change")
  void shouldThrowExceptionWhenRequestedServiceNotFoundForStatusChange() {
    when(requestedServiceRepository.findById(any())).thenReturn(Optional.empty());

    final var exception = assertThrows(ResourceNotFoundException.class,
        () -> requestedServiceService.changeStatusTOcancelOrDone(0L, RequestedServiceStatusEnum.CANCELLED));

    assertEquals("Requested service not found.", exception.getMessage());
    verify(requestedServiceRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should find requested services by user ID successfully")
  void shouldFindRequestedServiceByUserIdSuccessfully() {
    final var userId = 1L;
    final var pageable = PageRequest.of(0, 10);
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address, List.of());
    final var requestedServiceResponseDto = RequestedServiceMapper.INSTANCE
        .requestedServiceToRequestedServiceResponseDto(requestedService);
    final var requestedServicePage = new PageImpl<>(List.of(requestedService), pageable, 1);
    final var expectedResponsePage = new PageImpl<>(List.of(requestedServiceResponseDto), pageable, 1);

    when(requestedServiceRepository.findByUserId(userId, pageable)).thenReturn(requestedServicePage);

    final var result = requestedServiceService.findByUserId(userId, pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(expectedResponsePage.getContent().get(0).id(), result.getContent().get(0).id());
    verify(requestedServiceRepository, times(1)).findByUserId(userId, pageable);
  }

  @Test
  @DisplayName("Should return empty page when no requested services found for user ID")
  void shouldReturnEmptyPageWhenNoRequestedServiceForUserFound() {
    final var userId = 0L;
    final var pageable = PageRequest.of(0, 10);
    final Page<RequestedService> emptyPage = Page.empty(pageable);
    final var expectedEmptyResponsePage = Page.empty(pageable);

    when(requestedServiceRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

    final var result = requestedServiceService.findByUserId(userId, pageable);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
    assertEquals(expectedEmptyResponsePage, result);
    verify(requestedServiceRepository, times(1)).findByUserId(userId, pageable);
  }

  @Test
  @DisplayName("Should throw exception when trying to change status to an invalid type (e.g., INPROGRESS)")
  void shouldThrowExceptionWhenTryingToChangeStatusToInvalidType() {
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
}
