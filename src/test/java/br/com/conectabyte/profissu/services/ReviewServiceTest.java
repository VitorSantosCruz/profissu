package br.com.conectabyte.profissu.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.entities.Review;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.repositories.ReviewRepository;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private UserService userService;

  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  void shouldRegisterReviewSuccessfullyWhenRequestedServiceIsDone() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, null);

    requestedService.setStatus(RequestedServiceStatusEnum.DONE);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
      Review savedReview = invocation.getArgument(0);
      savedReview.setId(10L);
      return savedReview;
    });

    var response = reviewService.register(1L, reviewRequestDto);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.title()).isEqualTo("Title");
    assertThat(response.review()).isEqualTo("Review");
    assertThat(response.stars()).isEqualTo(5);
  }

  @Test
  void shouldThrowValidationExceptionWhenRequestedServiceIsNotDone() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, null);

    requestedService.setStatus(RequestedServiceStatusEnum.INPROGRESS);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Feedback can only be provided for services that have been completed.");
  }

  @Test
  void shouldThrowExceptionWhenTokenDoesNotHaveSubClaim() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);

    when(jwtService.getClaims()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldThrowExceptionWhenUserNotFound() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenThrow(new IllegalArgumentException("User not found"));

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void shouldThrowExceptionWhenRequestedServiceNotFound() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var user = UserUtils.create();

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceService.findById(any()))
        .thenThrow(new IllegalArgumentException("RequestedService not found"));

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("RequestedService not found");
  }

  @Test
  void shouldPropagateExceptionWhenReviewRepositoryFails() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, null);

    requestedService.setStatus(RequestedServiceStatusEnum.DONE);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(reviewRepository.save(any(Review.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error");
  }

  @Test
  void shouldReturnReviewsGivenByUserWhenIsReviewOwnerTrue() {
    final var pageable = PageRequest.of(0, 10);
    final var review = new Review();

    review.setId(100L);

    final var reviewPage = new PageImpl<>(List.of(review), pageable, 1);

    when(reviewRepository.findReviewsGivenByUserId(any(), any())).thenReturn(reviewPage);

    final var result = reviewService.findByUserId(1L, true, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).id()).isEqualTo(100L);
  }

  @Test
  void shouldReturnReviewsReceivedByUserWhenIsReviewOwnerFalse() {
    final var pageable = PageRequest.of(0, 10);
    final var review = new Review();

    review.setId(200L);

    final var reviewPage = new PageImpl<>(List.of(review), pageable, 1);

    when(reviewRepository.findReviewsReceivedByUserId(any(), any())).thenReturn(reviewPage);

    final var result = reviewService.findByUserId(1L, false, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).id()).isEqualTo(200L);
  }

  @Test
  void shouldReturnEmptyPageWhenNoReviewsFound() {
    final var pageable = PageRequest.of(0, 10);
    final Page<Review> emptyPage = Page.empty(pageable);

    when(reviewRepository.findReviewsGivenByUserId(any(), any())).thenReturn(emptyPage);

    final var result = reviewService.findByUserId(1L, true, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
  }

}
