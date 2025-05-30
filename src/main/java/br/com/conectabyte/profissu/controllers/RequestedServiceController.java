package br.com.conectabyte.profissu.controllers;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/requested-services")
@RequiredArgsConstructor
@Tag(name = "Requested services", description = "Operations related to managing requested services")
public class RequestedServiceController {
  private final RequestedServiceService requestedServiceService;

  @Operation(summary = "List requested services with pagination", description = "Retrieve a paginated list of requested services.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successful retrieval of requested services", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @GetMapping
  public Page<RequestedServiceResponseDto> findAvailableServiceRequests(@ParameterObject Pageable pageable) {
    return requestedServiceService.findAvailableServiceRequests(pageable);
  }

  @Operation(summary = "Retrieve requested services by user ID", description = "Fetches a paginated list of requested services associated with the provided user ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved requested services", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @GetMapping("/by-user")
  public Page<RequestedServiceResponseDto> findByUserId(@RequestParam Long userId,
      @ParameterObject Pageable pageable) {
    return requestedServiceService.findByUserId(userId, pageable);
  }

  @Operation(summary = "Register a requested service", description = "Allows a user to register a new requested service.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Successfully created requested service", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestedServiceResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PostMapping
  public ResponseEntity<RequestedServiceResponseDto> register(
      @Valid @RequestBody RequestedServiceRequestDto requestedServiceRequestDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(this.requestedServiceService.register(requestedServiceRequestDto));
  }

  @Operation(summary = "Cancel a requested service", description = "Allows an authorized user to cancel a requested service.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully canceled the requested service", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestedServiceResponseDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Requested service not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
  })
  @PreAuthorize("@securityRequestedServiceService.ownershipCheck(#id)")
  @PatchMapping("/{id}/cancel")
  public ResponseEntity<RequestedServiceResponseDto> cancel(@PathVariable Long id) {
    return ResponseEntity.ok(requestedServiceService.cancel(id));
  }
}
