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

    final var savedReview = reviewRepository.save(review);

    sendNotification(savedReview);

    return reviewMapper.reviewToReviewResponseDto(savedReview);
  }

  @Transactional
  public Page<ReviewResponseDto> findByUserId(Long userId, boolean isReviewOwner, Pageable pageable) {
    final var reviews = isReviewOwner ? reviewRepository.findReviewsGivenByUserId(userId, pageable)
        : reviewRepository.findReviewsReceivedByUserId(userId, pageable);

    return reviewMapper.reviewPageToReviewResponseDtoPage(reviews);
  }

  public ReviewResponseDto updateById(Long id, ReviewRequestDto reviewRequestDto) {
    final var review = this.findById(id);

    review.setUpdatedAt(LocalDateTime.now());
    review.setReview(reviewRequestDto.review());
    review.setStars(reviewRequestDto.stars());
    review.setTitle(reviewRequestDto.title());

    final var updatedReview = reviewRepository.save(review);

    sendNotification(updatedReview);

    return reviewMapper.reviewToReviewResponseDto(updatedReview);
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

  private void sendNotification(Review review) {
    try {
      final var conversation = review.getRequestedService().getConversations().stream()
          .filter(c -> c.getOfferStatus() == OfferStatusEnum.ACCEPTED)
          .findAny()
          .orElseThrow();

      var receiver = conversation.getRequester();

      if (receiver.getId() == review.getUser().getId()) {
        receiver = conversation.getServiceProvider();
      }

      final var contact = receiver.getContacts().stream()
          .filter(Contact::isStandard)
          .filter(c -> c.getVerificationCompletedAt() != null)
          .findAny()
          .orElseThrow();

      final var notification = String.format(
          "%s, %s sent you a review about %s.",
          receiver.getName(),
          review.getUser().getName(),
          conversation.getRequestedService().getTitle());

      notificationService.send(new NotificationEmailDto(notification, contact.getValue()));
    } catch (Exception e) {
      log.warn("Unable to send notification");
    }
  }
}
