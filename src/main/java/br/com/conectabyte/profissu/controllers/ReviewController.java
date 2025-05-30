package br.com.conectabyte.profissu.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ExceptionDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.services.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Operations related to managing reviews")
public class ReviewController {
  private final ReviewService reviewService;

  @Operation(summary = "Register review", description = "Registers a new review for a requested service.", responses = {
      @ApiResponse(responseCode = "200", description = "Review successfully registered", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Requested service not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PostMapping
  public ResponseEntity<ReviewResponseDto> register(
      @RequestParam Long requestedServiceId,
      @Valid @RequestBody ReviewRequestDto reviewRequestDto) {
    return ResponseEntity.ok().body(this.reviewService.register(requestedServiceId, reviewRequestDto));
  }
}
