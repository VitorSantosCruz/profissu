package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.RequestedServiceCancellationException;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.mappers.RequestedServiceMapper;
import br.com.conectabyte.profissu.repositories.RequestedServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestedServiceService {
  private final RequestedServiceRepository requestedServiceRepository;
  private final UserService userService;

  private final RequestedServiceMapper requestedServiceMapper = RequestedServiceMapper.INSTANCE;

  public RequestedService findById(Long id) {
    final var optionalRequestedService = requestedServiceRepository.findById(id);
    final var requestedService = optionalRequestedService
        .orElseThrow(() -> new ResourceNotFoundException("Requested service not found."));

    return requestedService;
  }

  @Transactional
  public Page<RequestedServiceResponseDto> findByPage(Pageable pageable) {
    return requestedServiceMapper
        .RequestedServicePageToRequestedServiceResponseDtoPage(requestedServiceRepository.findAll(pageable));
  }

  @Transactional
  public RequestedServiceResponseDto register(Long userId, RequestedServiceRequestDto requestedServiceRequestDto) {
    final var user = userService.findById(userId);
    final var requestedServiceToBeSaved = requestedServiceMapper
        .requestedServiceRequestDtoToRequestedService(requestedServiceRequestDto);

    requestedServiceToBeSaved.setStatus(RequestedServiceStatusEnum.PENDING);
    requestedServiceToBeSaved.setUser(user);

    final var requestedService = requestedServiceRepository.save(requestedServiceToBeSaved);

    return requestedServiceMapper.requestedServiceToRequestedServiceResponseDto(requestedService);
  }

  @Transactional
  public RequestedServiceResponseDto cancel(Long id) {
    final var requestedService = this.findById(id);

    if(!requestedService.canBeCancelled()) {
      throw new RequestedServiceCancellationException("Requested service can't be cancelled");
    }

    requestedService.setUpdatedAt(LocalDateTime.now());
    requestedService.setStatus(RequestedServiceStatusEnum.CANCELLED);

    final var updatedRequestedService = requestedServiceRepository.save(requestedService);

    return requestedServiceMapper.requestedServiceToRequestedServiceResponseDto(updatedRequestedService);
  }
}
