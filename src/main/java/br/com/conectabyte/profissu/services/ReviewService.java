package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.entities.Review;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ReviewMapper;
import br.com.conectabyte.profissu.repositories.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final UserService userService;
  private final RequestedServiceService requestedServiceService;
  private final JwtService jwtService;

  private final ReviewMapper reviewMapper = ReviewMapper.INSTANCE;

  public Review findById(Long id) {
    final var optionalReview = this.reviewRepository.findById(id);
    final var review = optionalReview.orElseThrow(() -> new ResourceNotFoundException("User not found."));

    return review;
  }

  @Transactional
  public ReviewResponseDto register(Long requestedServiceId, ReviewRequestDto reviewRequestDto) {
    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();
    final var user = userService.findById(userId);
    final var requestedService = requestedServiceService.findById(requestedServiceId);
    final var review = reviewMapper.reviewRequestDtoToReview(reviewRequestDto);

    if (requestedService.getStatus() != RequestedServiceStatusEnum.DONE) {
      throw new ValidationException("Feedback can only be provided for services that have been completed.");
    }

    review.setUser(user);
    review.setRequestedService(requestedService);

    return reviewMapper.reviewToReviewResponseDto(reviewRepository.save(review));
  }

  @Transactional
  public Page<ReviewResponseDto> findByUserId(Long userId, boolean isReviewOwner, Pageable pageable) {
    final var reviews = isReviewOwner ? reviewRepository.findReviewsGivenByUserId(userId, pageable)
        : reviewRepository.findReviewsReceivedByUserId(userId, pageable);

    return reviewMapper.reviewPageToReviewResponseDtoPage(reviews);
  }

  @Async
  @Transactional
  public void deleteById(Long id) {
    final var optionalReview = this.reviewRepository.findById(id);

    optionalReview.ifPresent(review -> {
      review.setDeletedAt(LocalDateTime.now());
      reviewRepository.save(review);
    });
  }
}
