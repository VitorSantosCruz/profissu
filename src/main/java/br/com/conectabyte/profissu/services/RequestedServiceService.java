package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.request.TitleEmailDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.RequestedServiceCancellationException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.RequestedServiceMapper;
import br.com.conectabyte.profissu.repositories.RequestedServiceRepository;
import br.com.conectabyte.profissu.services.email.RequestedServiceCancellationNotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestedServiceService {
  private final RequestedServiceRepository requestedServiceRepository;
  private final UserService userService;
  private final RequestedServiceCancellationNotificationService requestedServiceCancellationNotificationService;
  private final JwtService jwtService;

  private final RequestedServiceMapper requestedServiceMapper = RequestedServiceMapper.INSTANCE;

  public RequestedService findById(Long id) {
    final var optionalRequestedService = requestedServiceRepository.findById(id);
    final var requestedService = optionalRequestedService
        .orElseThrow(() -> new ResourceNotFoundException("Requested service not found."));

    return requestedService;
  }

  @Transactional
  public Page<RequestedServiceResponseDto> findAvailableServiceRequests(Pageable pageable) {
    final var availableServiceRequests = requestedServiceRepository.findAvailableServiceRequests(pageable);
    return requestedServiceMapper.requestedServicePageToRequestedServiceResponseDtoPage(availableServiceRequests);
  }

  @Transactional
  public Page<RequestedServiceResponseDto> findByUserId(Long userId, Pageable pageable) {
    final var userServiceRequests = requestedServiceRepository.findByUserId(userId, pageable);
    return requestedServiceMapper.requestedServicePageToRequestedServiceResponseDtoPage(userServiceRequests);
  }

  @Transactional
  public RequestedServiceResponseDto register(RequestedServiceRequestDto requestedServiceRequestDto) {
    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();
    final var user = userService.findById(userId);
    final var requestedServiceToBeSaved = requestedServiceMapper
        .requestedServiceRequestDtoToRequestedService(requestedServiceRequestDto);

    requestedServiceToBeSaved.setStatus(RequestedServiceStatusEnum.PENDING);
    requestedServiceToBeSaved.setUser(user);

    final var requestedService = requestedServiceRepository.save(requestedServiceToBeSaved);

    return requestedServiceMapper.requestedServiceToRequestedServiceResponseDto(requestedService);
  }

  @Transactional
  public RequestedServiceResponseDto changeStatusTOcancelOrDone(Long id,
      RequestedServiceStatusEnum requestedServiceStatusEnum) {
    final var requestedService = this.findById(id);

    canChangeStatus(requestedService, requestedServiceStatusEnum);

    requestedService.setUpdatedAt(LocalDateTime.now());
    requestedService.setStatus(requestedServiceStatusEnum);
    requestedService.getConversations().forEach(c -> c.getServiceProvider().getContacts().stream()
        .filter(Contact::isStandard)
        .forEach(contact -> requestedServiceCancellationNotificationService
            .send(new TitleEmailDto(requestedService.getTitle(), contact.getValue()))));

    final var updatedRequestedService = requestedServiceRepository.save(requestedService);

    return requestedServiceMapper.requestedServiceToRequestedServiceResponseDto(updatedRequestedService);
  }

  private void canChangeStatus(RequestedService requestedService,
      RequestedServiceStatusEnum requestedServiceStatusEnum) {
    if (requestedService.getStatus() != RequestedServiceStatusEnum.PENDING
        || requestedServiceStatusEnum == RequestedServiceStatusEnum.INPROGRESS
        || requestedServiceStatusEnum == RequestedServiceStatusEnum.DONE) {
      throw new RequestedServiceCancellationException("Requested service can't be cancelled/done");
    }
  }
}
