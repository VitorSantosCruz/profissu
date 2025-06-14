package br.com.conectabyte.profissu.controllers;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

  @Operation(summary = "Find reviews by user", description = "Retrieves reviews based on the user ID and whether the user is the author or the recipient of the reviews.", responses = {
      @ApiResponse(responseCode = "200", description = "Reviews successfully retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid parameters provided", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @GetMapping
  public Page<ReviewResponseDto> findByUserId(@RequestParam Long userId, @RequestParam boolean isReviewOwner,
      @ParameterObject Pageable pageable) {
    return reviewService.findByUserId(userId, isReviewOwner, pageable);
  }

  @Operation(summary = "Register review", description = "Registers a new review for a requested service.", responses = {
      @ApiResponse(responseCode = "200", description = "Review successfully registered", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "401", description = "Invalid or missing authentication credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Requested service not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PostMapping
  @PreAuthorize("@securityRequestedServiceService.ownershipCheck(#requestedServiceId) || @securityRequestedServiceService.isServiceProvider(#requestedServiceId)")
  public ResponseEntity<ReviewResponseDto> register(@RequestParam Long requestedServiceId,
      @Valid @RequestBody ReviewRequestDto reviewRequestDto) {
    return ResponseEntity.ok().body(this.reviewService.register(requestedServiceId, reviewRequestDto));
  }

  @Operation(summary = "Update review by ID", description = "Allows the owner of a review to update its content. Ownership is verified to ensure only the author can perform this action.", responses = {
      @ApiResponse(responseCode = "200", description = "Review successfully updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "403", description = "User is not authorized to update this review", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityReviewService.ownershipCheck(#id)")
  @PutMapping("{id}")
  public ResponseEntity<ReviewResponseDto> updateById(@PathVariable Long id,
      @Valid @RequestBody ReviewRequestDto reviewRequestDto) {
    return ResponseEntity.ok().body(this.reviewService.updateById(id, reviewRequestDto));
  }

  @Operation(summary = "Delete review", description = "Allows the user to delete a review they have submitted.", responses = {
      @ApiResponse(responseCode = "202", description = "Review successfully deleted"),
      @ApiResponse(responseCode = "403", description = "User is not authorized to delete this review", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "400", description = "Malformed ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class))),
      @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionDto.class)))
  })
  @PreAuthorize("@securityReviewService.ownershipCheck(#id) || @securityService.isAdmin()")
  @DeleteMapping("{id}")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    this.reviewService.deleteById(id);
    return ResponseEntity.accepted().build();
  }
}
