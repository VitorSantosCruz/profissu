package br.com.conectabyte.profissu.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/requested-services")
@RequiredArgsConstructor
public class RequestedServiceController {
  private final RequestedServiceService requestedServiceService;

  @GetMapping
  public Page<RequestedServiceResponseDto> findByPage(Pageable pageable) {
    return requestedServiceService.findByPage(pageable);
  }

  @PostMapping("/{userId}")
  public ResponseEntity<RequestedServiceResponseDto> register(@PathVariable Long userId,
      @Valid @RequestBody RequestedServiceRequestDto requestedServiceRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.requestedServiceService.register(userId, requestedServiceRequestDto));
  }
}
