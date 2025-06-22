package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.EnumSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.request.TitleEmailDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.RequestedServiceCancellationException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.RequestedServiceMapper;
import br.com.conectabyte.profissu.repositories.RequestedServiceRepository;
import br.com.conectabyte.profissu.services.email.RequestedServiceCancellationNotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestedServiceService {
  private final RequestedServiceRepository requestedServiceRepository;
  private final UserService userService;
  private final RequestedServiceCancellationNotificationService requestedServiceCancellationNotificationService;
  private final JwtService jwtService;

  private final RequestedServiceMapper requestedServiceMapper = RequestedServiceMapper.INSTANCE;

  public RequestedService findById(Long id) {
    log.debug("Attempting to find requested service by ID: {}", id);

    final var optionalRequestedService = requestedServiceRepository.findById(id);
    final var requestedService = optionalRequestedService
        .orElseThrow(() -> {
          log.warn("Requested service with ID: {} not found.", id);
          return new ResourceNotFoundException("Requested service not found.");
        });

    log.debug("Found requested service with ID: {}", requestedService.getId());
    return requestedService;
  }

  @Transactional
  public Page<RequestedServiceResponseDto> findAvailableServiceRequests(Pageable pageable) {
    log.debug("Finding available service requests with pageable: {}", pageable);

    final var availableServiceRequests = requestedServiceRepository.findAvailableServiceRequests(pageable);

    log.debug("Found {} available service requests.", availableServiceRequests.getTotalElements());
    return requestedServiceMapper.requestedServicePageToRequestedServiceResponseDtoPage(availableServiceRequests);
  }

  @Transactional
  public Page<RequestedServiceResponseDto> findByUserId(Long userId, Pageable pageable) {
    log.debug("Finding requested services by user ID: {} with pageable: {}", userId, pageable);

    final var userServiceRequests = requestedServiceRepository.findByUserId(userId, pageable);

    log.debug("Found {} requested services for user ID: {}", userServiceRequests.getTotalElements(), userId);
    return requestedServiceMapper.requestedServicePageToRequestedServiceResponseDtoPage(userServiceRequests);
  }

  @Transactional
  public RequestedServiceResponseDto register(RequestedServiceRequestDto requestedServiceRequestDto) {
    log.debug("Registering new requested service with data: {}", requestedServiceRequestDto);

    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("Retrieved user ID from JWT: {}", userId);

    final var user = userService.findById(userId);
    final var requestedServiceToBeSaved = requestedServiceMapper
        .requestedServiceRequestDtoToRequestedService(requestedServiceRequestDto);

    requestedServiceToBeSaved.setStatus(RequestedServiceStatusEnum.PENDING);
    requestedServiceToBeSaved.setUser(user);

    final var requestedService = requestedServiceRepository.save(requestedServiceToBeSaved);

    log.info("Requested service registered successfully with ID: {} for user: {}", requestedService.getId(),
        user.getId());
    return requestedServiceMapper.requestedServiceToRequestedServiceResponseDto(requestedService);
  }

  @Transactional
  public RequestedServiceResponseDto changeStatusTOcancelOrDone(Long id,
      RequestedServiceStatusEnum requestedServiceStatusEnum) {
    log.debug("Attempting to change status for requested service ID: {} to: {}", id, requestedServiceStatusEnum);

    final var requestedService = this.findById(id);

    log.debug("Found requested service ID: {} with current status: {}", id, requestedService.getStatus());

    canChangeStatus(requestedService, requestedServiceStatusEnum);

    requestedService.setUpdatedAt(LocalDateTime.now());
    requestedService.setStatus(requestedServiceStatusEnum);

    final var updatedRequestedService = requestedServiceRepository.save(requestedService);

    log.info("Requested service ID: {} status changed successfully to: {}", updatedRequestedService.getId(),
        updatedRequestedService.getStatus());

    if (requestedServiceStatusEnum == RequestedServiceStatusEnum.CANCELLED) {
      log.debug("Notifying service providers about cancellation of service ID: {}", updatedRequestedService.getId());
      updatedRequestedService.getConversations()
          .stream()
          .filter(c -> c.getOfferStatus() != OfferStatusEnum.CANCELLED)
          .filter(c -> c.getOfferStatus() != OfferStatusEnum.REJECTED)
          .forEach(c -> c.getServiceProvider().getContacts().stream()
              .filter(Contact::isStandard)
              .forEach(contact -> {
                log.debug("Sending cancellation notification to service provider contact: {} for service ID: {}",
                    contact.getValue(), updatedRequestedService.getId());
                requestedServiceCancellationNotificationService
                    .send(new TitleEmailDto(updatedRequestedService.getTitle(), contact.getValue()));
              }));
      log.debug("Finished sending cancellation notifications for service ID: {}", updatedRequestedService.getId());
    }

    return requestedServiceMapper.requestedServiceToRequestedServiceResponseDto(updatedRequestedService);
  }

  private void canChangeStatus(RequestedService requestedService,
      RequestedServiceStatusEnum requestedServiceStatusEnum) {
    log.debug("Validating status change for requested service ID: {} to new status: {}", requestedService.getId(),
        requestedServiceStatusEnum);

    final var shouldBlockFinalize = requestedServiceStatusEnum == RequestedServiceStatusEnum.DONE
        && requestedService.getStatus() != RequestedServiceStatusEnum.INPROGRESS;
    final var shouldBlockCancel = requestedServiceStatusEnum == RequestedServiceStatusEnum.CANCELLED
        && requestedService.getStatus() != RequestedServiceStatusEnum.PENDING;

    if (shouldBlockFinalize || shouldBlockCancel) {
      log.warn("Validation failed for requested service ID {}: Attempted invalid status change from {} to {}.",
          requestedService.getId(), requestedService.getStatus(), requestedServiceStatusEnum);
      throw new RequestedServiceCancellationException("Requested service can't be cancelled/done");
    }

    if (!EnumSet.of(RequestedServiceStatusEnum.DONE, RequestedServiceStatusEnum.CANCELLED)
        .contains(requestedServiceStatusEnum)) {
      log.warn("Validation failed for requested service ID {}: Invalid target status {}.", requestedService.getId(),
          requestedServiceStatusEnum);
      throw new RequestedServiceCancellationException("Status is not valid");
    }

    log.debug("Status change validation successful for requested service ID: {}", requestedService.getId());
  }
}
