package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.NotificationEmailDto;
import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.Review;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ReviewMapper;
import br.com.conectabyte.profissu.repositories.ReviewRepository;
import br.com.conectabyte.profissu.services.email.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final UserService userService;
  private final RequestedServiceService requestedServiceService;
  private final JwtService jwtService;
  private final NotificationService notificationService;

  private final ReviewMapper reviewMapper = ReviewMapper.INSTANCE;

  public Review findById(Long id) {
    log.debug("Attempting to find review by ID: {}", id);

    final var optionalReview = this.reviewRepository.findById(id);
    final var review = optionalReview.orElseThrow(() -> {
      log.warn("Review with ID: {} not found.", id);
      return new ResourceNotFoundException("Review not found.");
    });

    log.debug("Found review with ID: {}", review.getId());
    return review;
  }

  @Transactional
  public ReviewResponseDto register(Long requestedServiceId, ReviewRequestDto reviewRequestDto) {
    log.debug("Registering new review for requested service ID: {} with data: {}", requestedServiceId,
        reviewRequestDto);

    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("Retrieved user ID from JWT: {}", userId);

    final var user = userService.findById(userId);
    final var requestedService = requestedServiceService.findById(requestedServiceId);
    final var review = reviewMapper.reviewRequestDtoToReview(reviewRequestDto);

    if (requestedService.getStatus() != RequestedServiceStatusEnum.DONE) {
      log.warn(
          "Validation failed: Cannot register review for requested service ID {} because its status is not DONE (current status: {}).",
          requestedServiceId, requestedService.getStatus());
      throw new ValidationException("Feedback can only be provided for services that have been completed.");
    }

    review.setUser(user);
    review.setRequestedService(requestedService);

    final var savedReview = reviewRepository.save(review);

    log.info("Review registered successfully with ID: {} for requested service ID: {}", savedReview.getId(),
        requestedServiceId);

    sendNotification(savedReview);
    log.debug("Notification sent for newly registered review ID: {}", savedReview.getId());
    return reviewMapper.reviewToReviewResponseDto(savedReview);
  }

  @Transactional
  public Page<ReviewResponseDto> findByUserId(Long userId, boolean isReviewOwner, Pageable pageable) {
    log.debug("Finding reviews by user ID: {}. Is review owner: {}. Pageable: {}", userId, isReviewOwner, pageable);

    final var reviews = isReviewOwner ? reviewRepository.findReviewsGivenByUserId(userId, pageable)
        : reviewRepository.findReviewsReceivedByUserId(userId, pageable);

    log.debug("Found {} reviews for user ID: {} (isReviewOwner: {}).", reviews.getTotalElements(), userId,
        isReviewOwner);
    return reviewMapper.reviewPageToReviewResponseDtoPage(reviews);
  }

  public ReviewResponseDto updateById(Long id, ReviewRequestDto reviewRequestDto) {
    log.debug("Updating review with ID: {} with data: {}", id, reviewRequestDto);

    final var review = this.findById(id);

    log.debug("Found review to update: {}", review.getId());

    review.setUpdatedAt(LocalDateTime.now());
    review.setReview(reviewRequestDto.review());
    review.setStars(reviewRequestDto.stars());
    review.setTitle(reviewRequestDto.title());

    final var updatedReview = reviewRepository.save(review);

    log.info("Review with ID: {} updated successfully.", updatedReview.getId());

    sendNotification(updatedReview);
    log.debug("Notification sent for updated review ID: {}", updatedReview.getId());
    return reviewMapper.reviewToReviewResponseDto(updatedReview);
  }

  @Async
  @Transactional
  public void deleteById(Long id) {
    log.debug("Attempting to delete review by ID: {}", id);

    final var optionalReview = this.reviewRepository.findById(id);

    optionalReview.ifPresent(review -> {
      review.setDeletedAt(LocalDateTime.now());
      reviewRepository.save(review);
      log.info("Review with ID: {} soft-deleted successfully.", id);
    });

    if (optionalReview.isEmpty()) {
      log.warn("Attempted to delete review with ID: {} but it was not found.", id);
    }
  }

  private void sendNotification(Review review) {
    log.debug("Preparing to send notification for review ID: {}", review.getId());

    try {
      final var conversation = review.getRequestedService().getConversations().stream()
          .filter(c -> c.getOfferStatus() == OfferStatusEnum.ACCEPTED)
          .findAny()
          .orElseThrow();

      log.debug("Found accepted conversation for review ID {}: Conversation ID {}", review.getId(),
          conversation.getId());

      var receiver = conversation.getRequester();

      if (receiver.getId() == review.getUser().getId()) {
        receiver = conversation.getServiceProvider();
        log.debug("Receiver is service provider for review ID {}: User ID {}", review.getId(), receiver.getId());
      } else {
        log.debug("Receiver is requester for review ID {}: User ID {}", review.getId(), receiver.getId());
      }

      final var contact = receiver.getContacts().stream()
          .filter(Contact::isStandard)
          .filter(c -> c.getVerificationCompletedAt() != null)
          .findAny()
          .orElseThrow();

      log.debug("Found standard contact for receiver user ID {}: Contact value {}", receiver.getId(),
          contact.getValue());

      final var notification = String.format(
          "%s, %s sent you a review about %s.",
          receiver.getName(),
          review.getUser().getName(),
          conversation.getRequestedService().getTitle());

      log.debug("Notification message generated: {}", notification);

      notificationService.send(new NotificationEmailDto(notification, contact.getValue()));
      log.info("Notification sent for review ID: {} to email: {}", review.getId(), contact.getValue());
    } catch (Exception e) {
      log.warn("Unable to send notification for review ID {}: {}", review.getId(), e.getMessage());
    }
  }
}
