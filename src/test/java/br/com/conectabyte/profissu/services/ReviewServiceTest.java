package br.com.conectabyte.profissu.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.entities.Review;
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
  void shouldRegisterReviewSuccessfully() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var user = UserUtils.create();
    final var requestedService = RequestedServiceUtils.create(user, null);

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

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(reviewRepository.save(any(Review.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error");
  }
}
