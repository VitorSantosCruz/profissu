package br.com.conectabyte.profissu.controllers;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.RequestedServiceRequestDto;
import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.dtos.response.RequestedServiceResponseDto;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/requested-services")
@RequiredArgsConstructor
public class RequestedServiceController {
  private final RequestedServiceService requestedServiceService;

  @Operation(summary = "List requested services with pagination", description = "Retrieve a paginated list of requested services.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successful retrieval of requested services", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @GetMapping
  public Page<RequestedServiceResponseDto> findByPage(@ParameterObject Pageable pageable) {
    return requestedServiceService.findByPage(pageable);
  }

  @Operation(summary = "Register a requested service", description = "Allows a user to register a new requested service.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Successfully created requested service", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestedServiceResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PreAuthorize("@securityService.isOwner(#id) || @securityService.isAdmin()")
  @PostMapping("/{userId}")
  public ResponseEntity<RequestedServiceResponseDto> register(@PathVariable Long userId,
      @Valid @RequestBody RequestedServiceRequestDto requestedServiceRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.requestedServiceService.register(userId, requestedServiceRequestDto));
  }
}
