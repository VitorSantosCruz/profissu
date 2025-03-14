package br.com.conectabyte.profissu.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
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
}
